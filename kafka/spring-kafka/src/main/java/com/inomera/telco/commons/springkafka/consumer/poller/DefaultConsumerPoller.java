package com.inomera.telco.commons.springkafka.consumer.poller;

import com.inomera.telco.commons.lang.Assert;
import com.inomera.telco.commons.lang.PropertyUtils;
import com.inomera.telco.commons.lang.thread.FutureUtils;
import com.inomera.telco.commons.lang.thread.IncrementalNamingThreadFactory;
import com.inomera.telco.commons.lang.thread.ThreadUtils;
import com.inomera.telco.commons.springkafka.consumer.KafkaConsumerProperties;
import com.inomera.telco.commons.springkafka.consumer.KafkaMessageConsumer;
import com.inomera.telco.commons.springkafka.consumer.PollerThreadState;
import com.inomera.telco.commons.springkafka.consumer.ConsumerThreadStore;
import com.inomera.telco.commons.springkafka.util.HostUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Serdar Kuzucu
 */
public class DefaultConsumerPoller implements ConsumerPoller, Runnable, ConsumerRebalanceListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConsumerPoller.class);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Map<TopicPartition, List<Future<ConsumerRecord<String, ?>>>> inProgressMessages = new HashMap<>();
    private final Map<TopicPartition, OffsetAndMetadata> offsetMap = new HashMap<>(1);

    private final KafkaConsumerProperties kafkaConsumerProperties;
    private final ThreadFactory consumerThreadFactory;
    private final Deserializer<?> valueDeserializer;
    private ConsumerRecordHandler consumerRecordHandler;
    private ExecutorService executorService;
    private KafkaConsumer<String, ?> consumer;
    private boolean autoPartitionPause;
    private final ConsumerThreadStore threadStore;

    public DefaultConsumerPoller(KafkaConsumerProperties kafkaConsumerProperties,
                                 ThreadFactory consumerThreadFactory, Deserializer<?> valueDeserializer,
                                 boolean autoPartitionPause,
                                 ConsumerThreadStore threadStore) {
        this.kafkaConsumerProperties = kafkaConsumerProperties;
        this.consumerThreadFactory = consumerThreadFactory;
        this.valueDeserializer = valueDeserializer;
        this.autoPartitionPause = autoPartitionPause;
        this.threadStore = threadStore;
    }

    @Override
    public void run() {
        try {
            if (kafkaConsumerProperties.hasPatternBasedTopic()) {
                consumer.subscribe(kafkaConsumerProperties.getTopicPattern(), this);
            } else {
                consumer.subscribe(kafkaConsumerProperties.getTopics(), this);
            }

            List<Future<ConsumerRecord<String, ?>>> partitionFutures;

            final Collection<TopicPartition> toBePause = new HashSet<>();
            final Collection<TopicPartition> toBeResume = new HashSet<>();
            TopicPartition tp;
            int pollWaitMs = 3000;
            pollLoop:
            while (running.get()) {
                final ConsumerRecords<String, ?> records = consumer.poll(pollWaitMs);

                if (!records.isEmpty()) {
                    pollWaitMs = 0;
                    if (kafkaConsumerProperties.isAtMostOnceBulk() && !commitLastOffsets()) {
                        continue;
                    }

                    toBePause.clear();
                    for (ConsumerRecord<String, ?> rec : records) {
                        if (kafkaConsumerProperties.isAtMostOnceSingle() && !commitOffset(rec)) {
                            continue pollLoop;
                        }

                        tp = new TopicPartition(rec.topic(), rec.partition());
                        partitionFutures = inProgressMessages.get(tp);
                        try {
                            partitionFutures.add(consumerRecordHandler.handle(rec));
                            if (autoPartitionPause) {
                                toBePause.add(new TopicPartition(rec.topic(), rec.partition()));
                            }
                        } catch (Exception e) {
                            LOGGER.error("Error processing kafka message [{}].", rec.value(), e);
                        }
                    }

                    // do not read any records on the next poll cycle
                    LOGGER.debug("PAUSED-> {}", toBePause);
                    consumer.pause(toBePause);
                } else {
                    // increase poll wait time until 3sec
                    pollWaitMs = Math.min(pollWaitMs + 10, 3000);
                }

                toBeResume.clear();

                for (Map.Entry<TopicPartition, List<Future<ConsumerRecord<String, ?>>>> e : inProgressMessages.entrySet()) {
                    if (!e.getValue().isEmpty()) {
                        final ConsumerRecord<String, ?> lastCompleted = getLastCompletedRecord(e.getValue());

                        if (lastCompleted != null && kafkaConsumerProperties.isAtLeastOnceSingle()) {
                            commitOffset(lastCompleted);
                        }

                        if (e.getValue().isEmpty()) {
                            if (kafkaConsumerProperties.isAtLeastOnceBulk()) {
                                if (lastCompleted != null) {
                                    commitOffset(lastCompleted);
                                }
                            }
                            toBeResume.add(e.getKey());
                        }
                    }
                }

                if (!toBeResume.isEmpty()) {
                    consumer.resume(toBeResume);
                    LOGGER.debug("RESUMED-> {}", toBePause);
                }
            }
        } catch (WakeupException e) {
            LOGGER.info("WakeupException is handled. Shutting down the consumer.");
            if (running.get()) {
                LOGGER.error("WakeupException occurred while running=true! " +
                        "This may be an error therefore here is the stacktrace for error {}", e.getMessage(), e);
                throw e;
            }
        } catch (Exception e) {
            LOGGER.error("Exception exception occurred when polling or committing, message : {}",
                    e.getMessage(), e);
        } finally {
            consumer.close();
        }

        closed.set(true);
    }

    private ConsumerRecord<String, ?> getLastCompletedRecord(List<Future<ConsumerRecord<String, ?>>> invocations) {
        ConsumerRecord<String, ?> lastCompleted = null;
        ConsumerRecord<String, ?> crTemp;

        final Iterator<Future<ConsumerRecord<String, ?>>> iterator = invocations.iterator();

        while (iterator.hasNext()) {
            final Future<ConsumerRecord<String, ?>> nextRecord = iterator.next();
            if (nextRecord.isDone()) {
                if (lastCompleted == null) {
                    lastCompleted = FutureUtils.getUnchecked(nextRecord);
                } else if (lastCompleted.offset() < (crTemp = FutureUtils.getUnchecked(nextRecord)).offset()) {
                    lastCompleted = crTemp;
                }
                iterator.remove();
            } else {
                break;
            }
        }

        return lastCompleted;
    }

    private boolean commitLastOffsets() {
        try {
            consumer.commitSync();
            return true;
        } catch (CommitFailedException e) {
            LOGGER.info("Committing last offsets failed for {}.", kafkaConsumerProperties.getClientId(), e);
            return false;
        }
    }

    private boolean commitOffset(ConsumerRecord<String, ?> rec) {
        try {
            offsetMap.clear();
            offsetMap.put(new TopicPartition(rec.topic(), rec.partition()),
                    new OffsetAndMetadata(rec.offset() + 1));
            consumer.commitSync(offsetMap);
            return true;
        } catch (CommitFailedException e) {
            LOGGER.error("Offset commit failed for {}. offset={}, request={}",
                    kafkaConsumerProperties.getClientId(), offsetMap, rec.value(), e);
            return false;
        }
    }

    @Override
    public void start(KafkaMessageConsumer kafkaMessageConsumer) {
        Assert.notNull(consumerRecordHandler, "ConsumerRecordHandler is null!");
        LOGGER.info("Starting consumer. group={}", kafkaConsumerProperties.getGroupId());
        this.consumer = new KafkaConsumer<>(buildConsumerProperties(), new StringDeserializer(), valueDeserializer);
        this.executorService = Executors.newCachedThreadPool(consumerThreadFactory);
        executorService.submit(this);
        running.set(true);

        if (consumerThreadFactory instanceof IncrementalNamingThreadFactory) {
            final IncrementalNamingThreadFactory consumerThreadFactory = (IncrementalNamingThreadFactory) this.consumerThreadFactory;
            for (Thread thread : Thread.getAllStackTraces().keySet()) {
                if (StringUtils.startsWithIgnoreCase(thread.getName(), consumerThreadFactory.getThreadNamePrefix())) {
                    final String hostname = HostUtils.getHostname();
                    final String hostedThreadName = hostname.concat("-").concat(thread.getName());
                    final long threadId = thread.getId();

                    final PollerThreadState pollerThreadState = new PollerThreadState();
                    pollerThreadState.setThreadId(threadId);
                    pollerThreadState.setHostname(hostname);
                    pollerThreadState.setThreadName(hostedThreadName);
                    pollerThreadState.setOldJvmState(thread.getState().name());
                    pollerThreadState.setCurrentJvmState(thread.getState().name());
                    pollerThreadState.setKafkaMessageConsumer(kafkaMessageConsumer);

                    threadStore.put(threadId, pollerThreadState);
                }
            }
        }

    }

    @Override
    public void stop() {
        if (!running.get()) {
            return;
        }

        this.running.set(false);
        this.consumer.wakeup();
        do {
            ThreadUtils.sleepQuietly(500);
        } while (!closed.get());

        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Override
    public void pause(TopicPartition topicPartition) {
        consumer.pause(Collections.singletonList(topicPartition));
    }

    private Properties buildConsumerProperties() {
        final Properties props = PropertyUtils.copyProperties(kafkaConsumerProperties.getKafkaConsumerProperties());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConsumerProperties.getGroupId());
        if (kafkaConsumerProperties.hasPatternBasedTopic()) {
            /*
             * We are decreasing the default metadata load period, which is 5
             * minutes, to 30s so that pattern based subscriptions take affect
             * shorter.
             */
            props.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, "30000");
        }
        props.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, kafkaConsumerProperties.getClientId());
        return PropertyUtils.overrideWithSystemArguments(props);
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        inProgressMessages.clear();
        for (TopicPartition tp : partitions) {
            inProgressMessages.put(tp, new ArrayList<>());
        }

        LOGGER.info("ASSIGNED-> {}", partitions);
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitionsRevoked) {
        if (!(kafkaConsumerProperties.isAtLeastOnce())) {
            return;
        }

        for (TopicPartition tpRevoked : partitionsRevoked) {
            final List<Future<ConsumerRecord<String, ?>>> inProgressTasks = inProgressMessages.get(tpRevoked);
            if (inProgressTasks != null && !inProgressTasks.isEmpty()) {
                for (int i = inProgressTasks.size() - 1; i >= 0; --i) {
                    final Future<ConsumerRecord<String, ?>> f = inProgressTasks.get(i);
                    if (f.isDone()) {
                        commitOffset(FutureUtils.getUnchecked(f));
                        break;
                    }
                }
            }
        }
        LOGGER.info("REVOKED-> {}", partitionsRevoked);
    }

    public void setConsumerRecordHandler(ConsumerRecordHandler consumerRecordHandler) {
        Assert.notNull(consumerRecordHandler, "ConsumerRecordHandler is null!");
        this.consumerRecordHandler = consumerRecordHandler;
    }
}
