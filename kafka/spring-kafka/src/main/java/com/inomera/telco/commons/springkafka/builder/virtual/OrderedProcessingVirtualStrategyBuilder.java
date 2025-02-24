package com.inomera.telco.commons.springkafka.builder.virtual;

import com.inomera.telco.commons.lang.thread.IncrementalNamingVirtualThreadFactory;
import com.inomera.telco.commons.springkafka.consumer.executor.virtual.PartitionKeyAwareVirtualExecutorStrategy;
import com.inomera.telco.commons.springkafka.consumer.executor.virtual.VirtualExecutorStrategy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ThreadFactory;

import static com.inomera.telco.commons.springkafka.SpringKafkaConstants.INVOKER_THREAD_NAME_FORMAT;

/**
 * @author Turgay Can
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class OrderedProcessingVirtualStrategyBuilder {
    private final VirtualConsumerInvokerBuilder virtualConsumerInvokerBuilder;
    private int numberOfThreads = 3;
    private ThreadFactory threadFactory;

    public OrderedProcessingVirtualStrategyBuilder numberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        return this;
    }

    public OrderedProcessingVirtualStrategyBuilder threadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        return this;
    }

    public VirtualConsumerInvokerBuilder and() {
        return this.virtualConsumerInvokerBuilder;
    }

    public VirtualExecutorStrategy build(String groupId) {
        final ThreadFactory threadFactory = getOrCreateInvokerThreadFactory(groupId);
        return new PartitionKeyAwareVirtualExecutorStrategy(numberOfThreads, threadFactory);
    }

    private ThreadFactory getOrCreateInvokerThreadFactory(String groupId) {
        if (this.threadFactory != null) {
            return this.threadFactory;
        }

        return new IncrementalNamingVirtualThreadFactory(String.format(INVOKER_THREAD_NAME_FORMAT, groupId));
    }
}
