package com.inomera.telco.commons.springkafka.builder;

import com.inomera.telco.commons.lang.thread.IncrementalNamingThreadFactory;
import com.inomera.telco.commons.springkafka.consumer.executor.ExecutorPerTopicStrategy;
import com.inomera.telco.commons.springkafka.consumer.executor.ExecutorStrategy;
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
public class ExecutorPerTopicStrategyBuilder extends AbstractExecutorStrategyBuilder {
    private final UnorderedProcessingStrategyBuilder unorderedProcessingStrategyBuilder;

    private int coreThreadCount = 0;
    private int maxThreadCount = 1;
    private int keepAliveTime = 1;
    private TimeUnit keepAliveTimeUnit = TimeUnit.MINUTES;
    private ThreadFactory threadFactory;
    private int queueCapacity = Integer.MAX_VALUE;

    public ExecutorPerTopicStrategyBuilder coreThreadCount(int coreThreadCount) {
        this.coreThreadCount = coreThreadCount;
        return this;
    }

    public ExecutorPerTopicStrategyBuilder maxThreadCount(int maxThreadCount) {
        this.maxThreadCount = maxThreadCount;
        return this;
    }

    public ExecutorPerTopicStrategyBuilder keepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
        return this;
    }

    public ExecutorPerTopicStrategyBuilder keepAliveTimeUnit(TimeUnit keepAliveTimeUnit) {
        this.keepAliveTimeUnit = keepAliveTimeUnit;
        return this;
    }

    public ExecutorPerTopicStrategyBuilder threadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        return this;
    }

    public ExecutorPerTopicStrategyBuilder queueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
        return this;
    }

    public UnorderedProcessingStrategyBuilder and() {
        return this.unorderedProcessingStrategyBuilder;
    }

    private ThreadFactory getThreadFactory(String groupId) {
        if (this.threadFactory != null) {
            return this.threadFactory;
        }
        return new IncrementalNamingThreadFactory(String.format(INVOKER_THREAD_NAME_FORMAT, groupId));
    }

    @Override
    ExecutorStrategy build(String groupId) {
        final int maxThreadCount = Math.max(this.maxThreadCount, coreThreadCount);
        final ExecutorPerTopicStrategy executorPerTopicStrategy = new ExecutorPerTopicStrategy(getThreadFactory(groupId), forCapacity(queueCapacity));
        executorPerTopicStrategy.reconfigure(coreThreadCount, maxThreadCount, keepAliveTime, keepAliveTimeUnit);
        return executorPerTopicStrategy;
    }
}
