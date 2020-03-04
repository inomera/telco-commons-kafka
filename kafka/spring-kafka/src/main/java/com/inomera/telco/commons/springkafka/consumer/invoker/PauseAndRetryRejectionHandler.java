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

import java.util.concurrent.*;

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
    public void handleReject(final ConsumerRecord<String, ?> record, final FutureTask<ConsumerRecord<String, ?>> futureTask) {
        final TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
        LOG.info("handleReject::rejected execution of {}", topicPartition);

        consumerPoller.pause(topicPartition);
        retryExecutor.submit(new RetryTask(futureTask, record));
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
        private final FutureTask<ConsumerRecord<String, ?>> futureTask;
        private final ConsumerRecord<String, ?> record;
        private int retryCount = 0;

        @Override
        public void run() {
            if (!futureTask.isDone()) {
                try {
                    final ThreadPoolExecutor executor = executorStrategy.get(record);
                    if (!executor.getQueue().offer(futureTask, 5, TimeUnit.MILLISECONDS)) {
                        retryCount++;
                        LOG.info("Queue is still full. retryCount={}, topic={}, partition={}",
                                retryCount, record.topic(), record.partition());
                        try {
                            retryExecutor.submit(this);
                        } catch (RejectedExecutionException e) {
                            LOG.info("handleReject::rejected execution, retryExecutor may be closing... {}", e.getMessage());
                            retryInLoop();
                        }
                    }
                } catch (InterruptedException e) {
                    LOG.info("handleReject::execution queue interrupted::{}-{}", record.topic(), record.partition());
                    Thread.currentThread().interrupt();
                }
            }
        }

        private void retryInLoop() {
            long start = System.currentTimeMillis();
            long elapsedTime = 0;

            while (!futureTask.isDone()) {
                try {
                    final ThreadPoolExecutor executor = executorStrategy.get(record);
                    if (!executor.getQueue().offer(futureTask, 100, TimeUnit.MILLISECONDS)) {
                        elapsedTime = System.currentTimeMillis() - start;
                        if (elapsedTime > 2000) {
                            LOG.info("handleReject::threads are busy more than {} seconds", elapsedTime / 1000);
                            start = System.currentTimeMillis();
                        }
                    }
                } catch (InterruptedException e) {
                    LOG.info("handleReject::execution queue interrupted");
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            LOG.info("handleReject::completed in {} millis", elapsedTime);
        }
    }
}
