package com.inomera.telco.commons.springkafka.consumer.invoker.virtual;

import com.inomera.telco.commons.lang.thread.IncrementalNamingThreadFactory;
import com.inomera.telco.commons.springkafka.consumer.executor.virtual.VirtualExecutorStrategy;
import com.inomera.telco.commons.springkafka.consumer.invoker.BulkInvokerResult;
import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;
import com.inomera.telco.commons.springkafka.consumer.invoker.RejectionHandler;
import com.inomera.telco.commons.springkafka.consumer.poller.ConsumerPoller;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.*;

/**
 * @author Turgay Can
 */
@RequiredArgsConstructor
public class VirtualPauseAndRetryRejectionHandler implements RejectionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(VirtualPauseAndRetryRejectionHandler.class);
    private static final BlockingQueue<Runnable> retryQueue = new LinkedBlockingQueue<>();

    private final ConsumerPoller consumerPoller;
    private final VirtualExecutorStrategy executorStrategy;

    private ExecutorService retryExecutor;

    @Override
    public void handleReject(final ConsumerRecord<String, ?> record, final FutureTask<InvokerResult> futureTask) {
        final TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
        LOG.info("simple handleReject::rejected execution of {}", topicPartition);

        consumerPoller.pause(topicPartition);
        retryExecutor.submit(new VirtualRetryTask(futureTask, record));
    }

    @Override
    public void handleReject(Set<ConsumerRecord<String, ?>> records, FutureTask<BulkInvokerResult> futureTask) {
        final ConsumerRecord<String, ?> firstRecord = records.iterator().next();
        final TopicPartition topicPartition = new TopicPartition(firstRecord.topic(), firstRecord.partition());
        LOG.info("bulk handleReject::rejected execution of {}", topicPartition);

        consumerPoller.pause(topicPartition);
        retryExecutor.submit(new VirtualBulkRetryTask(futureTask, records));
    }

    @Override
    public void stop() {
        retryExecutor.shutdown();
        retryExecutor = null;
    }

    @Override
    public void start() {
        IncrementalNamingThreadFactory threadFactory = new IncrementalNamingThreadFactory("invoker-rejection-retry-");
        retryExecutor = Executors.newThreadPerTaskExecutor(threadFactory);
    }

    @RequiredArgsConstructor
    public class VirtualRetryTask implements Runnable {
        private static final Logger LOG = LoggerFactory.getLogger(VirtualRetryTask.class);

        private final FutureTask<InvokerResult> futureTask;
        private final ConsumerRecord<String, ?> record;
        private final long initializationTimestamp = System.currentTimeMillis();


        @Override
        public void run() {
            if (!futureTask.isDone()) {
                try {
                    final ExecutorService executor = executorStrategy.getExecutor(record);
                    try {
                        executor.submit(futureTask);
                        return;
                    } catch (RejectedExecutionException e) {
                        LOG.debug("{} rejected by exception: {}", record.topic(), e.getMessage());
                    }

                    if (!retryQueue.offer(this, 50, TimeUnit.MILLISECONDS)) {
                        final long elapsedTime = System.currentTimeMillis() - initializationTimestamp;
                        if (elapsedTime > 3000) {
                            LOG.info("handleReject::{} threads are busy more than {} seconds. Retry queue size is {}",
                                    record.topic(), elapsedTime, retryQueue.size());
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
    public class VirtualBulkRetryTask implements Runnable {
        private static final Logger LOG = LoggerFactory.getLogger(VirtualBulkRetryTask.class);

        private final FutureTask<BulkInvokerResult> futureTask;
        private final Set<ConsumerRecord<String, ?>> records;
        private final long initializationTimestamp = System.currentTimeMillis();

        private static final BlockingQueue<Runnable> retryQueue = new LinkedBlockingQueue<>();

        @Override
        public void run() {
            if (!futureTask.isDone()) {
                final ConsumerRecord<String, ?> firstRecord = records.iterator().next();
                try {
                    final ExecutorService executor = executorStrategy.getExecutor(firstRecord);
                    try {
                        executor.submit(futureTask);
                        return;
                    } catch (RejectedExecutionException e) {
                        LOG.debug("bulk {} rejected by exception: {}", firstRecord.topic(), e.getMessage());
                    }

                    if (!retryQueue.offer(this, 50, TimeUnit.MILLISECONDS)) {
                        final long elapsedTime = System.currentTimeMillis() - initializationTimestamp;
                        if (elapsedTime > 3000) {
                            LOG.info("bulk handleReject::{} threads are busy more than {} seconds. Retry queue size is {}",
                                    firstRecord.topic(), elapsedTime, retryQueue.size());
                        }
                        retryExecutor.submit(this);
                    }
                } catch (InterruptedException e) {
                    LOG.info("bulk handleReject::execution queue interrupted::{}-{}", firstRecord.topic(), firstRecord.partition());
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
