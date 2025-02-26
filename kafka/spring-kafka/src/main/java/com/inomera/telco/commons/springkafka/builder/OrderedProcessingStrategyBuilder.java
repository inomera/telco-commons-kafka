package com.inomera.telco.commons.springkafka.builder;

import com.inomera.telco.commons.lang.thread.IncrementalNamingThreadFactory;
import com.inomera.telco.commons.springkafka.consumer.executor.ExecutorStrategy;
import com.inomera.telco.commons.springkafka.consumer.executor.PartitionKeyAwareExecutorStrategy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ThreadFactory;

import static com.inomera.telco.commons.springkafka.SpringKafkaConstants.INVOKER_THREAD_NAME_FORMAT;

/**
 * @author Serdar Kuzucu
 * @author Turgay Can
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class OrderedProcessingStrategyBuilder {
    private final ConsumerInvokerBuilder consumerInvokerBuilder;
    private AbstractExecutorStrategyBuilder executorStrategyBuilder;

    private int numberOfThreads = 3;
    private ThreadFactory threadFactory;

    public OrderedProcessingStrategyBuilder numberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        return this;
    }

    public OrderedProcessingStrategyBuilder threadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        return this;
    }

    public ConsumerInvokerBuilder custom(ExecutorStrategy executorStrategy) {
        this.executorStrategyBuilder = new FixedInstanceExecutorStrategyBuilder(executorStrategy);
        return consumerInvokerBuilder;
    }

    public ConsumerInvokerBuilder and() {
        return this.consumerInvokerBuilder;
    }

    ExecutorStrategy build(String groupId) {
        if (executorStrategyBuilder != null) {
            return this.executorStrategyBuilder.build(groupId);
        }
        final ThreadFactory threadFactory = getOrCreateInvokerThreadFactory(groupId);
        return new PartitionKeyAwareExecutorStrategy(numberOfThreads, threadFactory);
    }

    private ThreadFactory getOrCreateInvokerThreadFactory(String groupId) {
        if (this.threadFactory != null) {
            return this.threadFactory;
        }

        return new IncrementalNamingThreadFactory(String.format(INVOKER_THREAD_NAME_FORMAT, groupId));
    }
}
