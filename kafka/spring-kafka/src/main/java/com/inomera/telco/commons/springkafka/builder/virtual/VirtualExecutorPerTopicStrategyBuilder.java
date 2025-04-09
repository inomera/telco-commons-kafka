package com.inomera.telco.commons.springkafka.builder.virtual;

import com.inomera.telco.commons.springkafka.consumer.executor.virtual.ParameterBasedVirtualExecutorStrategy;
import com.inomera.telco.commons.springkafka.consumer.executor.virtual.PerTopicVirtualExecutorStrategy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ThreadFactory;

/**
 * @author Turgay Can
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class VirtualExecutorPerTopicStrategyBuilder implements VirtualExecutorStrategyBuilder {
    private final UnorderedVirtualProcessingStrategyBuilder unorderedVirtualProcessingStrategyBuilder;

    private ThreadFactory threadFactory;

    public VirtualExecutorPerTopicStrategyBuilder threadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        return this;
    }

    public UnorderedVirtualProcessingStrategyBuilder and() {
        return this.unorderedVirtualProcessingStrategyBuilder;
    }

    @Override
    public ParameterBasedVirtualExecutorStrategy build(String groupId) {
        return new PerTopicVirtualExecutorStrategy(getThreadFactory(this.threadFactory, groupId));
    }

}
