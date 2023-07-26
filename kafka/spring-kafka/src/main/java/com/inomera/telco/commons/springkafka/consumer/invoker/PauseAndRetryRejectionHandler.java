package com.inomera.telco.commons.springkafka.consumer.invoker;

import com.inomera.telco.commons.lang.thread.IncrementalNamingThreadFactory;
import com.inomera.telco.commons.springkafka.consumer.executor.ExecutorStrategy;
import com.inomera.telco.commons.springkafka.consumer.poller.ConsumerPoller;
import com.inomera.telco.commons.springkafka.util.ThreadPoolExecutorUtils;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Serdar Kuzucu
 */
@RequiredArgsConstructor
public class PauseAndRetryRejectionHandler implements RejectionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PauseAndRetryRejectionHandler.class);

    private final ConsumerPoller consumerPoller;
    private final ExecutorStrategy executorStrategy;
    private ThreadPoolExecutor retryExecutor;
    private int retryPoolCoreThreadCount = 0;
    private int retryPoolMaxThreadCount = 4;
    private int retryPoolKeepAliveTime = 1;
    private TimeUnit retryPoolKeepAliveTimeUnit = TimeUnit.MINUTES;

    @Override
    public void handleReject(final ConsumerRecord<String, ?> record, final FutureTask<InvokerResult> futureTask) {
        final TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
        LOG.info("handleReject::rejected execution of {}", topicPartition);

        consumerPoller.pause(topicPartition);
        retryExecutor.submit(new RetryTask(futureTask, record));
    }

    @Override
    public void handleReject(Set<ConsumerRecord<String, ?>> records, FutureTask<BulkInvokerResult> futureTask) {
        final ConsumerRecord<String, ?> firstRecord = records.iterator().next();
        final TopicPartition topicPartition = new TopicPartition(firstRecord.topic(), firstRecord.partition());
        LOG.info("handleReject::rejected execution of {}", topicPartition);

        consumerPoller.pause(topicPartition);
        retryExecutor.submit(new BulkRetryTask(futureTask, records));
    }

    @Override
    public void stop() {
        ThreadPoolExecutorUtils.closeGracefully(retryExecutor, LOG, getClass().getSimpleName());
        retryExecutor = null;
    }

    @Override
    public void start() {
        retryExecutor = new ThreadPoolExecutor(retryPoolCoreThreadCount, retryPoolMaxThreadCount,
                retryPoolKeepAliveTime, retryPoolKeepAliveTimeUnit, new LinkedBlockingQueue<>(),
                new IncrementalNamingThreadFactory("invoker-rejection-retry-"));
    }

    public void setRetryPoolCoreThreadCount(int retryPoolCoreThreadCount) {
        this.retryPoolCoreThreadCount = retryPoolCoreThreadCount;
        this.retryExecutor.setCorePoolSize(retryPoolCoreThreadCount);
    }

    public void setRetryPoolMaxThreadCount(int retryPoolMaxThreadCount) {
        this.retryPoolMaxThreadCount = retryPoolMaxThreadCount;
        this.retryExecutor.setMaximumPoolSize(retryPoolMaxThreadCount);
    }

    public void setRetryPoolKeepAliveTime(int retryPoolKeepAliveTime, TimeUnit retryPoolKeepAliveTimeUnit) {
        this.retryPoolKeepAliveTime = retryPoolKeepAliveTime;
        this.retryPoolKeepAliveTimeUnit = retryPoolKeepAliveTimeUnit;
        this.retryExecutor.setKeepAliveTime(retryPoolKeepAliveTime, retryPoolKeepAliveTimeUnit);
    }

    @RequiredArgsConstructor
    private class RetryTask implements Runnable {
        private final FutureTask<InvokerResult> futureTask;
        private final ConsumerRecord<String, ?> record;
        private long initializationTimestamp = System.currentTimeMillis();

        @Override
        public void run() {
            if (!futureTask.isDone()) {
                try {
                    final ThreadPoolExecutor executor = executorStrategy.get(record);
                    try {
                        /*
                         * submit(Runnable) method creates new threads when possible.
                         * We need to first try this. Since this is not blocking, it will
                         * throw RejectedExecutionException when threads are busy and queue is full.
                         */
                        executor.submit(futureTask);
                        return;
                    } catch (RejectedExecutionException e) {
                        LOG.debug("{} rejected by exception: {}", record.topic(), e.getMessage());
                    }
                    if (!executor.getQueue().offer(futureTask, 5, TimeUnit.MILLISECONDS)) {
                        final long elapsedTime = System.currentTimeMillis() - initializationTimestamp;
                        if (elapsedTime > 2000) {
                            LOG.info("handleReject::{} threads are busy more than {} seconds. Retry queue size is {}",
                                    record.topic(), elapsedTime, retryExecutor.getQueue().size());
                        }
                        retryExecutor.submit(this);
                    }
                } catch (InterruptedException e) {
                    LOG.info("handleReject::execution queue interrupted::{}-{}", record.topic(), record.partition());
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @RequiredArgsConstructor
    private class BulkRetryTask implements Runnable {
        private final FutureTask<BulkInvokerResult> futureTask;
        private final Set<ConsumerRecord<String, ?>> records;
        private long initializationTimestamp = System.currentTimeMillis();

        @Override
        public void run() {
            if (!futureTask.isDone()) {
                final ConsumerRecord<String, ?> firstRecord = records.iterator().next();
                try {
                    final ThreadPoolExecutor executor = executorStrategy.get(firstRecord);
                    try {
                        /*
                         * submit(Runnable) method creates new threads when possible.
                         * We need to first try this. Since this is not blocking, it will
                         * throw RejectedExecutionException when threads are busy and queue is full.
                         */
                        executor.submit(futureTask);
                        return;
                    } catch (RejectedExecutionException e) {
                        LOG.debug("{} rejected by exception: {}", firstRecord.topic(), e.getMessage());
                    }
                    if (!executor.getQueue().offer(futureTask, 5, TimeUnit.MILLISECONDS)) {
                        final long elapsedTime = System.currentTimeMillis() - initializationTimestamp;
                        if (elapsedTime > 2000) {
                            LOG.info("handleReject::{} threads are busy more than {} seconds. Retry queue size is {}",
                                    firstRecord.topic(), elapsedTime, retryExecutor.getQueue().size());
                        }
                        retryExecutor.submit(this);
                    }
                } catch (InterruptedException e) {
                    LOG.info("handleReject::execution queue interrupted::{}-{}", firstRecord.topic(), firstRecord.partition());
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
