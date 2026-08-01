package com.inomera.telco.commons.springkafka.builder.virtual;

import com.inomera.telco.commons.springkafka.consumer.executor.virtual.BoundedVirtualExecutorStrategy;
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

    /**
     * Virtual threads with a hard ceiling on concurrent listener invocations.
     *
     * <p>Prefer this over {@link #singleExecutor()} for IO-bound listeners: a plain
     * thread-per-task executor never rejects, so nothing limits how many records of a poll batch
     * run at once and the downstream absorbs the whole batch.
     *
     * @param maxConcurrency in-flight invocation cap
     */
    public VirtualConsumerInvokerBuilder boundedExecutor(int maxConcurrency) {
        return custom(new BoundedVirtualExecutorStrategy(maxConcurrency));
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
