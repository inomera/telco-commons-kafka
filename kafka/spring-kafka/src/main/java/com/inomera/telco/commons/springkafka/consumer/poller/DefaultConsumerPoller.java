package com.inomera.telco.commons.springkafka.consumer.poller;

import com.inomera.telco.commons.lang.Assert;
import com.inomera.telco.commons.lang.PropertyUtils;
import com.inomera.telco.commons.lang.thread.FutureUtils;
import com.inomera.telco.commons.lang.thread.IncrementalNamingThreadFactory;
import com.inomera.telco.commons.lang.thread.ThreadUtils;
import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import com.inomera.telco.commons.springkafka.consumer.KafkaConsumerProperties;
import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;
import com.inomera.telco.commons.springkafka.util.InterruptUtils;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Serdar Kuzucu
 */
public class DefaultConsumerPoller implements ConsumerPoller, Runnable, ConsumerRebalanceListener {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultConsumerPoller.class);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Map<TopicPartition, List<Future<InvokerResult>>> inProgressMessages = new HashMap<>();
    private final Map<TopicPartition, OffsetAndMetadata> offsetMap = new HashMap<>(1);

    private final KafkaConsumerProperties kafkaConsumerProperties;
    private final Deserializer<?> valueDeserializer;
    private ThreadFactory consumerThreadFactory;
    private ConsumerRecordHandler consumerRecordHandler;
    private ExecutorService executorService;
    private KafkaConsumer<String, ?> consumer;
    private final boolean autoPartitionPause;

    public DefaultConsumerPoller(KafkaConsumerProperties kafkaConsumerProperties,
                                 Deserializer<?> valueDeserializer,
                                 boolean autoPartitionPause) {
        this.kafkaConsumerProperties = kafkaConsumerProperties;
        this.valueDeserializer = valueDeserializer;
        this.autoPartitionPause = autoPartitionPause;
    }

    @Override
    public void run() {
        try {
            if (kafkaConsumerProperties.hasPatternBasedTopic()) {
                consumer.subscribe(kafkaConsumerProperties.getTopicPattern(), this);
            } else {
                consumer.subscribe(kafkaConsumerProperties.getTopics(), this);
            }

            List<Future<InvokerResult>> partitionFutures;

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
                            LOG.error("Error processing kafka message [{}].", rec.value(), e);
                            InterruptUtils.interruptIfInterruptedException(e);
                        }
                    }

                    // do not read any records on the next poll cycle
                    LOG.debug("PAUSED-> {}", toBePause);
                    consumer.pause(toBePause);
                } else {
                    // increase poll wait time until 3sec
                    pollWaitMs = Math.min(pollWaitMs + 10, 3000);
                }

                toBeResume.clear();

                final Iterator<Map.Entry<TopicPartition, List<Future<InvokerResult>>>> messageIterator = inProgressMessages.entrySet().iterator();
                while (messageIterator.hasNext()) {
                    final Map.Entry<TopicPartition, List<Future<InvokerResult>>> messageEntry = messageIterator.next();
                    final List<Future<InvokerResult>> messageEntryValue = messageEntry.getValue();
                    if (!messageEntryValue.isEmpty()) {
                        final InvokerResult lastCompleted = getLastCompletedRecord(messageEntryValue);
                        if (lastCompleted != null) {
                            final KafkaListener kafkaListener = lastCompleted.getKafkaListener();
                            if (kafkaListener != null && kafkaListener.retry()) {
                                LOG.warn("message remove without commit for processing the topic : {}, if the consumer re-start or re-subscribe another consumer in consumer group, try to process", messageEntry.getKey().topic());
                                messageIterator.remove();
                            }
                        }
                        if (lastCompleted != null && kafkaConsumerProperties.isAtLeastOnceSingle()) {
                            commitOffset(lastCompleted.getRecord());
                        }

                        if (messageEntryValue.isEmpty()) {
                            if (kafkaConsumerProperties.isAtLeastOnceBulk()) {
                                if (lastCompleted != null) {
                                    commitOffset(lastCompleted.getRecord());
                                }
                            }
                            toBeResume.add(messageEntry.getKey());
                        }
                    }
                }
                if (!toBeResume.isEmpty()) {
                    consumer.resume(toBeResume);
                    LOG.debug("RESUMED-> {}", toBePause);
                }
            }
        } catch (WakeupException e) {
            LOG.info("WakeupException is handled. Shutting down the consumer.");
            if (running.get()) {
                LOG.error("WakeupException occurred while running=true! " +
                        "This may be an error therefore here is the stacktrace for error {}", e.getMessage(), e);
            }
        } catch (Exception e) {
            LOG.error("Exception occurred when polling or committing, message : {}",
                    e.getMessage(), e);
            InterruptUtils.interruptIfInterruptedException(e);
        } finally {
            try {
                consumer.close();
            } catch (Exception e) {
                LOG.error("Exception while closing consumer {}: {}",
                        kafkaConsumerProperties.getGroupId(), e.getMessage(), e);
                InterruptUtils.interruptIfInterruptedException(e);
            }
        }

        closed.set(true);
    }

    private InvokerResult getLastCompletedRecord(List<Future<InvokerResult>> invocations) {
        InvokerResult lastCompleted = null;
        InvokerResult crTemp;

        final Iterator<Future<InvokerResult>> iterator = invocations.iterator();

        while (iterator.hasNext()) {
            final Future<InvokerResult> nextRecord = iterator.next();
            if (nextRecord.isDone()) {
                if (lastCompleted == null) {
                    lastCompleted = FutureUtils.getUnchecked(nextRecord);
                } else if (lastCompleted.getRecord().offset() < (crTemp = FutureUtils.getUnchecked(nextRecord)).getRecord().offset()) {
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
            LOG.info("Committing last offsets failed for {}.", kafkaConsumerProperties.getClientId(), e);
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
            LOG.error("Offset commit failed for {}. offset={}, request={}",
                    kafkaConsumerProperties.getClientId(), offsetMap, rec.value(), e);
            return false;
        }
    }

    @Override
    public void start() {
        Assert.notNull(consumerRecordHandler, "ConsumerRecordHandler is null!");
        LOG.info("Starting consumer. group={}", kafkaConsumerProperties.getGroupId());
        shutdownExecutorIfNotNull();
        closeConsumerSilently();
        this.consumer = new KafkaConsumer<>(buildConsumerProperties(), new StringDeserializer(), valueDeserializer);
        final ThreadFactory threadFactory = this.consumerThreadFactory == null
                ? new IncrementalNamingThreadFactory(kafkaConsumerProperties.getGroupId()) : this.consumerThreadFactory;
        this.executorService = new ThreadPoolExecutor(0, 1, 0, TimeUnit.MILLISECONDS, new SynchronousQueue<>(), threadFactory);
        executorService.submit(this);
        running.set(true);
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

        shutdownExecutorIfNotNull();
    }

    private void shutdownExecutorIfNotNull() {
        if (this.executorService != null) {
            this.executorService.shutdownNow();
            this.executorService = null;
        }
    }

    private void closeConsumerSilently() {
        if (consumer == null) {
            return;
        }
        try {
            consumer.close();
        } catch (Exception e) {
            LOG.trace("Exception in closeConsumerSilently: {}", e.getMessage(), e);
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

        LOG.info("ASSIGNED-> {}", partitions);
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitionsRevoked) {
        if (!(kafkaConsumerProperties.isAtLeastOnce())) {
            return;
        }

        for (TopicPartition tpRevoked : partitionsRevoked) {
            final List<Future<InvokerResult>> inProgressTasks = inProgressMessages.get(tpRevoked);
            if (inProgressTasks != null && !inProgressTasks.isEmpty()) {
                for (int i = inProgressTasks.size() - 1; i >= 0; --i) {
                    final Future<InvokerResult> f = inProgressTasks.get(i);
                    if (f.isDone()) {
                        commitOffset(FutureUtils.getUnchecked(f).getRecord());
                        break;
                    }
                }
            }
        }
        LOG.info("REVOKED-> {}", partitionsRevoked);
    }

    public void setConsumerRecordHandler(ConsumerRecordHandler consumerRecordHandler) {
        Assert.notNull(consumerRecordHandler, "ConsumerRecordHandler is null!");
        this.consumerRecordHandler = consumerRecordHandler;
    }

    public void setConsumerThreadFactory(ThreadFactory consumerThreadFactory) {
        this.consumerThreadFactory = consumerThreadFactory;
    }

    @Override
    public boolean shouldRestart() {
        return this.running.get() && this.closed.get();
    }
}
