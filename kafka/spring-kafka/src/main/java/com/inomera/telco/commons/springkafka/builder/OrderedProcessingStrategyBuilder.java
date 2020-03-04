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
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class OrderedProcessingStrategyBuilder {
    private final ConsumerInvokerBuilder consumerInvokerBuilder;
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

    public ConsumerInvokerBuilder and() {
        return this.consumerInvokerBuilder;
    }

    private ThreadFactory getOrCreateInvokerThreadFactory(String groupId) {
        if (this.threadFactory != null) {
            return this.threadFactory;
        }

        return new IncrementalNamingThreadFactory(String.format(INVOKER_THREAD_NAME_FORMAT, groupId));
    }

    ExecutorStrategy build(String groupId) {
        final ThreadFactory threadFactory = getOrCreateInvokerThreadFactory(groupId);
        return new PartitionKeyAwareExecutorStrategy(numberOfThreads, threadFactory);
    }
}
