package com.inomera.telco.commons.springkafka.consumer.executor;

import com.inomera.telco.commons.springkafka.util.ThreadPoolExecutorUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * @author Serdar Kuzucu
 */
public class ExecutorPerTopicStrategy implements ExecutorStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(ExecutorPerTopicStrategy.class);

    private final Map<String, ThreadPoolExecutor> executorMap = new ConcurrentHashMap<>();
    private final Object lock = new Object();
    private final ThreadFactory threadFactory;
    private final Supplier<BlockingQueue<Runnable>> queueSupplier;
    private int coreThreadCount = 0;
    private int maxThreadCount = 1;
    private int keepAliveTime = 1;
    private TimeUnit keepAliveTimeUnit = TimeUnit.MINUTES;

    public ExecutorPerTopicStrategy(ThreadFactory threadFactory, Supplier<BlockingQueue<Runnable>> queueSupplier) {
        this.threadFactory = threadFactory;
        this.queueSupplier = queueSupplier;
    }

    @Override
    public ThreadPoolExecutor get(ConsumerRecord<String, ?> record) {
        createExecutorIfNotExist(record.topic());
        return executorMap.get(record.topic());
    }

    @Override
    public void start() {
        // nothing necessary
    }

    @Override
    public void stop() {
        synchronized (lock) {
            executorMap.values().forEach(ThreadPoolExecutor::shutdown);
            executorMap.forEach((executorName, executor) -> ThreadPoolExecutorUtils.closeGracefully(executor, LOG, executorName));
            executorMap.clear();
        }
    }

    public void reconfigure(int coreThreadCount, int maxThreadCount, int keepAliveTime, TimeUnit keepAliveTimeUnit) {
        synchronized (lock) {
            this.coreThreadCount = coreThreadCount;
            this.maxThreadCount = maxThreadCount;
            this.keepAliveTime = keepAliveTime;
            this.keepAliveTimeUnit = keepAliveTimeUnit;
            executorMap.forEach(this::reconfigureExecutor);
        }
    }

    private void reconfigureExecutor(String executorName, ThreadPoolExecutor executor) {
        executor.setCorePoolSize(coreThreadCount);
        executor.setMaximumPoolSize(maxThreadCount);
        executor.setKeepAliveTime(keepAliveTime, keepAliveTimeUnit);
        final int prestartedCoreThreads = executor.prestartAllCoreThreads();
        LOG.info("reconfigure::executorName={}, coreThreadCount={}, maxThreadCount={}, keepAliveTime={}, keepAliveTimeUnit={}, startedNewThreads={}",
                executorName, coreThreadCount, maxThreadCount, keepAliveTime, keepAliveTimeUnit, prestartedCoreThreads);
    }

    private void createExecutorIfNotExist(String executorName) {
        if (!executorMap.containsKey(executorName)) {
            // block if no executor exists for specified executorName
            synchronized (lock) {
                // re-check to avoid waiting threads to re-create executor again
                if (!executorMap.containsKey(executorName)) {
                    executorMap.put(executorName, createExecutor());
                }
            }
        }
    }

    private ThreadPoolExecutor createExecutor() {
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(coreThreadCount, maxThreadCount, keepAliveTime, keepAliveTimeUnit,
                queueSupplier.get(), threadFactory);
        threadPoolExecutor.prestartAllCoreThreads();
        return threadPoolExecutor;
    }
}
