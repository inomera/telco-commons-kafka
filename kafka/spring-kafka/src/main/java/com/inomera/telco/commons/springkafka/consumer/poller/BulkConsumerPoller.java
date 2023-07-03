package com.inomera.telco.commons.springkafka.consumer.poller;

import com.inomera.telco.commons.lang.Assert;
import com.inomera.telco.commons.lang.thread.FutureUtils;
import com.inomera.telco.commons.lang.thread.IncrementalNamingThreadFactory;
import com.inomera.telco.commons.lang.thread.ThreadUtils;
import com.inomera.telco.commons.springkafka.consumer.KafkaConsumerProperties;
import com.inomera.telco.commons.springkafka.consumer.invoker.BulkInvokerResult;
import com.inomera.telco.commons.springkafka.util.InterruptUtils;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


public class BulkConsumerPoller extends DefaultConsumerPoller {
    private static final Logger LOG = LoggerFactory.getLogger(BulkConsumerPoller.class);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Map<TopicPartition, List<Future<BulkInvokerResult>>> inProgressMessages = new HashMap<>();
    private final Map<TopicPartition, OffsetAndMetadata> offsetMap = new HashMap<>(1);
    private ThreadFactory consumerThreadFactory;
    private BulkConsumerRecordHandler consumerRecordHandler;
    private ExecutorService executorService;
    private KafkaConsumer<String, ?> consumer;
    private BulkRecordRetryer bulkRecordRetryer;

    public BulkConsumerPoller(KafkaConsumerProperties kafkaConsumerProperties,
			      Deserializer<?> valueDeserializer,
			      boolean autoPartitionPause) {
	super(kafkaConsumerProperties, valueDeserializer, autoPartitionPause);
    }

    @Override
    public void run() {
	try {
	    final KafkaConsumerProperties kafkaConsumerProperties = getKafkaConsumerProperties();
	    if (kafkaConsumerProperties.hasPatternBasedTopic()) {
		consumer.subscribe(kafkaConsumerProperties.getTopicPattern(), this);
	    } else {
		consumer.subscribe(kafkaConsumerProperties.getTopics(), this);
	    }

	    List<Future<BulkInvokerResult>> partitionFutures = null;

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
		    Map<TopicPartition, Set<ConsumerRecord<String, ?>>> tpRecordsMap = new HashMap<>();
		    for (ConsumerRecord<String, ?> rec : records) {
			if (kafkaConsumerProperties.isAtMostOnceSingle() && !commitOffset(rec)) {
			    continue pollLoop;
			}

			tp = new TopicPartition(rec.topic(), rec.partition());
			partitionFutures = inProgressMessages.get(tp);
			final List<? extends ConsumerRecord<String, ?>> tpRecords = records.records(tp);
			try {
			    final Set<ConsumerRecord<String, ?>> consumerRecords = tpRecordsMap.getOrDefault(tp, new LinkedHashSet<>());
			    final Set<ConsumerRecord<String, ?>> tpRecs = tpRecords.stream().collect(Collectors.toSet());
			    tpRecs.addAll(consumerRecords);
			    tpRecordsMap.put(tp, tpRecs);
			    if (isAutoPartitionPause()) {
				toBePause.add(new TopicPartition(rec.topic(), rec.partition()));
			    }
			} catch (Exception e) {
			    LOG.error("Error processing kafka message [{}].", rec.value(), e);
			    InterruptUtils.interruptIfInterruptedException(e);
			}
		    }

		    for (Set<ConsumerRecord<String, ?>> messages : tpRecordsMap.values()) {
			partitionFutures.add(consumerRecordHandler.handle(messages));
		    }

		    // do not read any records on the next poll cycle
		    LOG.debug("PAUSED-> {}", toBePause);
		    consumer.pause(toBePause);
		} else {
		    // increase poll wait time until 3sec
		    pollWaitMs = Math.min(pollWaitMs + 10, 3000);
		}

		toBeResume.clear();

		final Iterator<Map.Entry<TopicPartition, List<Future<BulkInvokerResult>>>> messageIterator = inProgressMessages.entrySet().iterator();
		while (messageIterator.hasNext()) {
		    final Map.Entry<TopicPartition, List<Future<BulkInvokerResult>>> messageEntry = messageIterator.next();
		    final List<Future<BulkInvokerResult>> messageEntryValue = messageEntry.getValue();
		    if (!messageEntryValue.isEmpty()) {
			final BulkInvokerResult lastCompleted = getLastCompletedRecord(messageEntryValue);
			if (lastCompleted != null) {
			    bulkRecordRetryer.checkAndRetry(lastCompleted);
			}
			if (lastCompleted != null && kafkaConsumerProperties.isAtLeastOnceSingle()) {
			    commitOffset(lastCompleted.getRecords().iterator().next());
			}

			if (messageEntryValue.isEmpty()) {
			    if (kafkaConsumerProperties.isAtLeastOnceBulk()) {
				if (lastCompleted != null) {
				    commitOffset(lastCompleted.getRecords().iterator().next());
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
	    LOG.info("WakeupException is handled. Shutting down the bulk consumer.");
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
		LOG.error("Exception while closing bulk consumer {}: {}",
			getKafkaConsumerProperties().getGroupId(), e.getMessage(), e);
		InterruptUtils.interruptIfInterruptedException(e);
	    }
	}

	closed.set(true);
    }

    protected BulkInvokerResult getLastCompletedRecord(List<Future<BulkInvokerResult>> invocations) {
	BulkInvokerResult lastCompleted = null;
	BulkInvokerResult crTemp;

	final Iterator<Future<BulkInvokerResult>> iterator = invocations.iterator();

	while (iterator.hasNext()) {
	    final Future<BulkInvokerResult> nextRecord = iterator.next();
	    if (nextRecord.isDone()) {
		if (lastCompleted == null) {
		    lastCompleted = FutureUtils.getUnchecked(nextRecord);
		} else if (lastCompleted.getRecords().iterator().next().offset() < (crTemp = FutureUtils.getUnchecked(nextRecord)).getRecords().iterator().next().offset()) {
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
	    LOG.info("Committing last offsets failed for {}.", getKafkaConsumerProperties().getClientId(), e);
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
		    getKafkaConsumerProperties().getClientId(), offsetMap, rec.value(), e);
	    return false;
	}
    }

    @Override
    public void start() {
	Assert.notNull(consumerRecordHandler, "BulkConsumerRecordHandler is null!");
	LOG.info("Starting bulk consumer. group={}", getKafkaConsumerProperties().getGroupId());
	shutdownExecutorIfNotNull();
	closeConsumerSilently();
	this.bulkRecordRetryer = new DefaultBulkRecordRetryer(consumerRecordHandler);
	this.consumer = new KafkaConsumer<>(buildConsumerProperties(), new StringDeserializer(), getValueDeserializer());
	final ThreadFactory threadFactory = this.consumerThreadFactory == null
		? new IncrementalNamingThreadFactory(getKafkaConsumerProperties().getGroupId()) : this.consumerThreadFactory;
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
	if (!(getKafkaConsumerProperties().isAtLeastOnce() || getKafkaConsumerProperties().isAtLeastOnceBulk())) {
	    return;
	}

	for (TopicPartition tpRevoked : partitionsRevoked) {
	    final List<Future<BulkInvokerResult>> inProgressTasks = inProgressMessages.get(tpRevoked);
	    if (inProgressTasks != null && !inProgressTasks.isEmpty()) {
		for (int i = inProgressTasks.size() - 1; i >= 0; --i) {
		    final Future<BulkInvokerResult> f = inProgressTasks.get(i);
		    if (f.isDone()) {
			commitOffset(FutureUtils.getUnchecked(f).getRecords().iterator().next());
			break;
		    }
		}
	    }
	}
	LOG.info("REVOKED-> {}", partitionsRevoked);
    }

    public void setConsumerRecordHandler(BulkConsumerRecordHandler bulkConsumerRecordHandler) {
	Assert.notNull(bulkConsumerRecordHandler, "BulkConsumerRecordHandler is null!");
	this.consumerRecordHandler = bulkConsumerRecordHandler;
    }

    public void setConsumerThreadFactory(ThreadFactory consumerThreadFactory) {
	this.consumerThreadFactory = consumerThreadFactory;
    }

    @Override
    public boolean shouldRestart() {
	return this.running.get() && this.closed.get();
    }
}
