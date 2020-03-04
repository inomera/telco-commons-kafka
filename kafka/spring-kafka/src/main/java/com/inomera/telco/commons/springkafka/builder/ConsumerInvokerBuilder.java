package com.inomera.telco.commons.springkafka.builder;

import com.inomera.telco.commons.lang.Assert;
import com.inomera.telco.commons.lang.thread.IncrementalNamingThreadFactory;
import com.inomera.telco.commons.springkafka.consumer.executor.ExecutorStrategy;
import com.inomera.telco.commons.springkafka.consumer.invoker.*;
import com.inomera.telco.commons.springkafka.consumer.poller.ConsumerPoller;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import static com.inomera.telco.commons.springkafka.SpringKafkaConstants.INVOKER_THREAD_NAME_FORMAT;

/**
 * @author Serdar Kuzucu
 */
@RequiredArgsConstructor
public class ConsumerInvokerBuilder {
    private final KafkaConsumerBuilder kafkaConsumerBuilder;
    private final ListenerMethodRegistry listenerMethodRegistry;

    private String invokerThreadNamePrefix;
    private ThreadFactory invokerThreadFactory;
    private List<ListenerInvocationInterceptor> interceptors = new ArrayList<>();
    private boolean orderGuarantee = true;

    private UnorderedProcessingStrategyBuilder unorderedProcessingStrategyBuilder;
    private OrderedProcessingStrategyBuilder orderedProcessingStrategyBuilder;

    public ConsumerInvokerBuilder interceptor(ListenerInvocationInterceptor interceptor) {
        Assert.notNull(interceptor, "interceptor is null");
        this.interceptors.add(interceptor);
        return this;
    }

    public ConsumerInvokerBuilder interceptors(Collection<ListenerInvocationInterceptor> interceptors) {
        Assert.notNull(interceptors, "interceptors is null");
        this.interceptors.addAll(interceptors);
        return this;
    }

    public ConsumerInvokerBuilder invokerThreadFactory(ThreadFactory invokerThreadFactory) {
        Assert.notNull(invokerThreadFactory, "invokerThreadFactory cannot be null");
        this.invokerThreadFactory = invokerThreadFactory;
        return this;
    }

    public ConsumerInvokerBuilder invokerThreadNamePrefix(String invokerThreadNamePrefix) {
        Assert.notNull(invokerThreadNamePrefix, "invokerThreadFactory cannot be null");
        this.invokerThreadNamePrefix = invokerThreadNamePrefix;
        return this;
    }

    public OrderedProcessingStrategyBuilder ordered() {
        this.orderGuarantee = true;
        this.orderedProcessingStrategyBuilder = new OrderedProcessingStrategyBuilder(this);
        return orderedProcessingStrategyBuilder;
    }

    public UnorderedProcessingStrategyBuilder unordered() {
        this.orderGuarantee = false;
        this.unorderedProcessingStrategyBuilder = new UnorderedProcessingStrategyBuilder(this);
        return unorderedProcessingStrategyBuilder;
    }

    public KafkaConsumerBuilder and() {
        return kafkaConsumerBuilder;
    }

    private ThreadFactory getOrCreateInvokerThreadFactory(String groupId) {
        if (this.invokerThreadFactory != null) {
            return this.invokerThreadFactory;
        }

        if (invokerThreadNamePrefix != null) {
            return new IncrementalNamingThreadFactory(invokerThreadNamePrefix);
        }

        return new IncrementalNamingThreadFactory(String.format(INVOKER_THREAD_NAME_FORMAT, groupId));
    }

    ConsumerInvoker build(ConsumerPoller consumerPoller, String groupId) {
        final MethodInvoker methodInvoker = new MethodInvoker(groupId, listenerMethodRegistry, interceptors);

        if (orderGuarantee) {
            final ExecutorStrategy executorStrategy = orderedProcessingStrategyBuilder.build(groupId);
            return new SimpleConsumerInvoker(methodInvoker, executorStrategy);
        }

        final ExecutorStrategy executorStrategy = unorderedProcessingStrategyBuilder.build(groupId);
        final PauseAndRetryRejectionHandler rejectionHandler = new PauseAndRetryRejectionHandler(consumerPoller,
                executorStrategy);
        return new RejectionAwareConsumerInvoker(methodInvoker, executorStrategy, rejectionHandler);
    }
}
