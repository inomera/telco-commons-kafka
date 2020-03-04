package com.inomera.telco.commons.springkafka.builder;

import com.inomera.telco.commons.lang.thread.IncrementalNamingThreadFactory;
import com.inomera.telco.commons.springkafka.consumer.executor.SinglePoolExecutorStrategy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.inomera.telco.commons.springkafka.SpringKafkaConstants.INVOKER_THREAD_NAME_FORMAT;
import static com.inomera.telco.commons.springkafka.builder.BlockingQueueSuppliers.forCapacity;

/**
 * @author Serdar Kuzucu
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class SinglePoolExecutorStrategyBuilder extends AbstractExecutorStrategyBuilder {
    private final UnorderedProcessingStrategyBuilder unorderedProcessingStrategyBuilder;
    private int coreThreadCount = 0;
    private int maxThreadCount = 3;
    private int keepAliveTime = 1;
    private TimeUnit keepAliveTimeUnit = TimeUnit.MINUTES;
    private int queueCapacity = Integer.MAX_VALUE;
    private ThreadFactory threadFactory;

    public SinglePoolExecutorStrategyBuilder coreThreadCount(int coreThreadCount) {
        this.coreThreadCount = coreThreadCount;
        return this;
    }

    public SinglePoolExecutorStrategyBuilder maxThreadCount(int maxThreadCount) {
        this.maxThreadCount = maxThreadCount;
        return this;
    }

    public SinglePoolExecutorStrategyBuilder keepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
        return this;
    }

    public SinglePoolExecutorStrategyBuilder keepAliveTimeUnit(TimeUnit keepAliveTimeUnit) {
        this.keepAliveTimeUnit = keepAliveTimeUnit;
        return this;
    }

    public SinglePoolExecutorStrategyBuilder queueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
        return this;
    }

    public SinglePoolExecutorStrategyBuilder threadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        return this;
    }

    public UnorderedProcessingStrategyBuilder and() {
        return unorderedProcessingStrategyBuilder;
    }

    private ThreadFactory getThreadFactory(String groupId) {
        if (this.threadFactory != null) {
            return this.threadFactory;
        }
        return new IncrementalNamingThreadFactory(String.format(INVOKER_THREAD_NAME_FORMAT, groupId));
    }

    @Override
    SinglePoolExecutorStrategy build(String groupId) {
        final ThreadFactory threadFactory = getThreadFactory(groupId);
        return new SinglePoolExecutorStrategy(coreThreadCount, maxThreadCount, keepAliveTime, keepAliveTimeUnit,
                threadFactory, forCapacity(queueCapacity));
    }
}
