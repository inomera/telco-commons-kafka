package com.inomera.telco.commons.springkafka.builder.virtual;

import com.inomera.telco.commons.springkafka.consumer.executor.virtual.VirtualExecutorStrategy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * @author Turgay Can
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class UnorderedVirtualProcessingStrategyBuilder {
    private final VirtualConsumerInvokerBuilder virtualConsumerInvokerBuilder;
    private VirtualExecutorStrategyBuilder executorStrategyBuilder;

    public DynamicNamedVirtualExecutorStrategyBuilder dynamicNamedExecutors() {
        final DynamicNamedVirtualExecutorStrategyBuilder executorStrategyBuilder = new DynamicNamedVirtualExecutorStrategyBuilder(this);
        this.executorStrategyBuilder = executorStrategyBuilder;
        return executorStrategyBuilder;
    }

    public SinglePoolVirtualExecutorStrategyBuilder singleExecutor() {
        final SinglePoolVirtualExecutorStrategyBuilder executorStrategyBuilder = new SinglePoolVirtualExecutorStrategyBuilder(this);
        this.executorStrategyBuilder = executorStrategyBuilder;
        return executorStrategyBuilder;
    }

    public VirtualExecutorPerTopicStrategyBuilder executorPerTopic() {
        final VirtualExecutorPerTopicStrategyBuilder executorStrategyBuilder = new VirtualExecutorPerTopicStrategyBuilder(this);
        this.executorStrategyBuilder = executorStrategyBuilder;
        return executorStrategyBuilder;
    }

    public VirtualConsumerInvokerBuilder custom(VirtualExecutorStrategy executorStrategy) {
        this.executorStrategyBuilder = new FixedInstanceVirtualExecutorStrategyBuilder(executorStrategy);
        return virtualConsumerInvokerBuilder;
    }

    public VirtualConsumerInvokerBuilder and() {
        return this.virtualConsumerInvokerBuilder;
    }

    public VirtualExecutorStrategy build(String groupId) {
        if (executorStrategyBuilder != null) {
            return this.executorStrategyBuilder.build(groupId);
        }

        return new SinglePoolVirtualExecutorStrategyBuilder(this).build(groupId);
    }
}
