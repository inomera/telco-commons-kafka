package com.inomera.telco.commons.springkafka.builder.virtual;

import com.inomera.telco.commons.springkafka.consumer.executor.virtual.SingleVirtualThreadPoolExecutorStrategy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ThreadFactory;

/**
 * @author Turgay Can
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class SinglePoolVirtualExecutorStrategyBuilder implements VirtualExecutorStrategyBuilder {
    private final UnorderedVirtualProcessingStrategyBuilder unorderedVirtualProcessingStrategyBuilder;
    private ThreadFactory threadFactory;

    public SinglePoolVirtualExecutorStrategyBuilder threadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        return this;
    }

    public UnorderedVirtualProcessingStrategyBuilder and() {
        return unorderedVirtualProcessingStrategyBuilder;
    }

    @Override
    public SingleVirtualThreadPoolExecutorStrategy build(String groupId) {
        final ThreadFactory threadFactory = getThreadFactory(this.threadFactory, groupId);
        return new SingleVirtualThreadPoolExecutorStrategy(threadFactory);
    }

}
