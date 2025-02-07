package com.inomera.telco.commons.springkafka.consumer.retry;

import com.inomera.telco.commons.lang.thread.IncrementalNamingThreadFactory;
import com.inomera.telco.commons.lang.thread.ThreadUtils;
import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;
import com.inomera.telco.commons.springkafka.consumer.poller.ConsumerRecordHandler;
import com.inomera.telco.commons.springkafka.util.InterruptUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.util.Date;
import java.util.Map;
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

public class DefaultInMemoryRecordRetryConsumer implements SmartLifecycle, InMemoryRecordRetryConsumer, Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultInMemoryRecordRetryConsumer.class);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public static final BlockingQueue<RetryContext> retryQueue = new LinkedBlockingQueue<>(10_000);
    private final Map<String, AtomicInteger> retryMap = new ConcurrentHashMap<>(10);

    private final ConsumerRecordHandler consumerRecordHandler;

    private ExecutorService executorService;

    public DefaultInMemoryRecordRetryConsumer(ConsumerRecordHandler consumerRecordHandler) {
        this.consumerRecordHandler = consumerRecordHandler;
    }

    @Override
    public void run() {
        try {
            int pollWaitMs = 100;
            while (running.get()) {
                final RetryContext retryContext = retryQueue.poll(pollWaitMs, TimeUnit.MILLISECONDS);
                if (retryContext == null) {
                    pollWaitMs = Math.min(pollWaitMs + 50, 3000);
                    continue;
                }
                consume(retryContext);
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
    public void consume(RetryContext retryContext) {
        final InvokerResult result = retryContext.getRetry();
        final ConsumerRecord<String, ?> record = result.getRecord();
        final String key = record.topic() + "-" + record.key() + "-" + record.offset();
        final AtomicInteger actualCount = retryMap.computeIfAbsent(key, mf -> new AtomicInteger(0));
        final int currentRetryCount = actualCount.incrementAndGet();
        final long now = new Date().getTime();
        if (currentRetryCount >= result.getKafkaListener().retryCount() && retryContext.isPassed(now)) {
            LOG.warn(" the first one of the messages : {} is reached the retry count : {}, limit for the topic : {}", record, currentRetryCount, record.topic());
            retryMap.remove(key);
            return;
        }
        LOG.info("retry count : {},  message for processing the topic : {}", currentRetryCount, record);
        final Future<InvokerResult> future = consumerRecordHandler.handle(record);
        try {
            final InvokerResult invokerResult = future.get();
            if (invokerResult.getKafkaListener() == null) {
                return;
            }
            retryQueue.offer(retryContext);
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
