package com.inomera.telco.commons.springkafka.builder;

import com.inomera.telco.commons.springkafka.consumer.executor.ExecutorStrategy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * @author Serdar Kuzucu
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class UnorderedProcessingStrategyBuilder {
    private final ConsumerInvokerBuilder consumerInvokerBuilder;
    private AbstractExecutorStrategyBuilder executorStrategyBuilder;

    public DynamicNamedExecutorStrategyBuilder dynamicNamedExecutors() {
        final DynamicNamedExecutorStrategyBuilder executorStrategyBuilder = new DynamicNamedExecutorStrategyBuilder(this);
        this.executorStrategyBuilder = executorStrategyBuilder;
        return executorStrategyBuilder;
    }

    public SinglePoolExecutorStrategyBuilder singleExecutor() {
        final SinglePoolExecutorStrategyBuilder executorStrategyBuilder = new SinglePoolExecutorStrategyBuilder(this);
        this.executorStrategyBuilder = executorStrategyBuilder;
        return executorStrategyBuilder;
    }

    public ExecutorPerTopicStrategyBuilder executorPerTopic() {
        final ExecutorPerTopicStrategyBuilder executorStrategyBuilder = new ExecutorPerTopicStrategyBuilder(this);
        this.executorStrategyBuilder = executorStrategyBuilder;
        return executorStrategyBuilder;
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

        return new SinglePoolExecutorStrategyBuilder(this).build(groupId);
    }
}
