package com.inomera.telco.commons.springkafka.consumer.retry;

import com.inomera.telco.commons.lang.thread.IncrementalNamingThreadFactory;
import com.inomera.telco.commons.lang.thread.ThreadUtils;
import com.inomera.telco.commons.springkafka.consumer.invoker.BulkInvokerResult;
import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;
import com.inomera.telco.commons.springkafka.consumer.poller.BulkConsumerRecordHandler;
import com.inomera.telco.commons.springkafka.util.InterruptUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.inomera.telco.commons.springkafka.SpringKafkaConstants.RETRY_THREAD_NAME_FORMAT;

public class DefaultInMemoryBulkRecordRetryConsumer implements SmartLifecycle, InMemoryBulkRecordRetryConsumer, Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultInMemoryBulkRecordRetryConsumer.class);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public static final BlockingQueue<BulkRetryContext> bulkRetryQueue = new LinkedBlockingQueue<>();
    private final Map<String, AtomicInteger> retryMap = new ConcurrentHashMap<>(10);


    private final BulkConsumerRecordHandler bulkConsumerRecordHandler;

    private ExecutorService executorService;

    public DefaultInMemoryBulkRecordRetryConsumer(BulkConsumerRecordHandler bulkConsumerRecordHandler) {
	this.bulkConsumerRecordHandler = bulkConsumerRecordHandler;
    }

    @Override
    public void run() {
	try {
	    int pollWaitMs = 100;
	    while (running.get()) {
		final BulkRetryContext bulkRetryContext = bulkRetryQueue.poll(pollWaitMs, TimeUnit.MILLISECONDS);
		if (bulkRetryContext == null) {
		    pollWaitMs = Math.min(pollWaitMs + 50, 3000);
		    continue;
		}
		consume(bulkRetryContext);
	    }

	} catch (Exception e) {
	    LOG.error("Exception occurred when polling or committing, message : {}",
		    e.getMessage(), e);
	    InterruptUtils.interruptIfInterruptedException(e);
	} finally {
	    closed.set(true);
	}

    }

    @Override
    public void consume(BulkRetryContext retryContext) {
	final BulkInvokerResult result = retryContext.getRetry();
	final Set<ConsumerRecord<String, ?>> records = result.getRecords();
	final ConsumerRecord<String, ?> firstRecord = records.iterator().next();
	final String key = firstRecord.topic() + "-" + firstRecord.key() + "-" + firstRecord.offset();
	final AtomicInteger actualCount = retryMap.computeIfAbsent(key, mf -> new AtomicInteger(0));
	final long now = new Date().getTime();
	final int currentRetryCount = actualCount.incrementAndGet();
	if (currentRetryCount >= result.getKafkaListener().retryCount() && retryContext.isPassed(now)) {
	    LOG.warn(" the first one of the messages : {} is reached the max retry count limit for the topic : {}", firstRecord, firstRecord.topic());
	    retryMap.remove(key);
	    return;
	}
	LOG.info("retry : {},  message for processing the topic : {}", actualCount, firstRecord.topic());
	final Future<BulkInvokerResult> future = bulkConsumerRecordHandler.handle(records);
	try {
	    final BulkInvokerResult bulkInvokerResult = future.get();
	    if (bulkInvokerResult.getKafkaListener() == null) {
		return;
	    }
	    bulkRetryQueue.offer(retryContext);
	} catch (Exception e) {
	    //swallowed exception
	}
    }

    @Override
    public void start() {
	final IncrementalNamingThreadFactory threadFactory = new IncrementalNamingThreadFactory(RETRY_THREAD_NAME_FORMAT);
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
	do {
	    ThreadUtils.sleepQuietly(500);
	} while (!closed.get());

    }

    @Override
    public boolean isRunning() {
	return this.running.get();
    }

    @Override
    public boolean isAutoStartup() {
	return this.running.get() && this.closed.get();
    }
}
