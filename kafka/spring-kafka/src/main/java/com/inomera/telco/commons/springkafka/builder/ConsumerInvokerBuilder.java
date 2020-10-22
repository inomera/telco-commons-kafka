package com.inomera.telco.commons.springkafka.builder;

import com.inomera.telco.commons.lang.Assert;
import com.inomera.telco.commons.springkafka.consumer.executor.ExecutorStrategy;
import com.inomera.telco.commons.springkafka.consumer.invoker.*;
import com.inomera.telco.commons.springkafka.consumer.invoker.fault.ListenerMethodNotFoundHandler;
import com.inomera.telco.commons.springkafka.consumer.invoker.fault.LoggingListenerMethodNotFoundHandler;
import com.inomera.telco.commons.springkafka.consumer.poller.ConsumerPoller;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Serdar Kuzucu
 */
@RequiredArgsConstructor
public class ConsumerInvokerBuilder {
    private final KafkaConsumerBuilder kafkaConsumerBuilder;
    private final ListenerMethodRegistry listenerMethodRegistry;

    private final List<ListenerInvocationInterceptor> interceptors = new ArrayList<>();
    private boolean orderGuarantee = true;
    private ListenerMethodNotFoundHandler listenerMethodNotFoundHandler;

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

    public ConsumerInvokerBuilder listenerMethodNotFoundHandler(
            ListenerMethodNotFoundHandler listenerMethodNotFoundHandler) {
        Assert.notNull(listenerMethodNotFoundHandler, "listenerMethodNotFoundHandler is null");
        this.listenerMethodNotFoundHandler = listenerMethodNotFoundHandler;
        return this;
    }

    public KafkaConsumerBuilder and() {
        return kafkaConsumerBuilder;
    }

    private ListenerMethodNotFoundHandler getOrCreateListenerMethodNotFoundHandler() {
        if (this.listenerMethodNotFoundHandler != null) {
            return this.listenerMethodNotFoundHandler;
        }
        return new LoggingListenerMethodNotFoundHandler();
    }

    private MethodInvoker buildMethodInvoker(String groupId) {
        return new MethodInvoker(groupId, listenerMethodRegistry, interceptors,
                getOrCreateListenerMethodNotFoundHandler());
    }

    ConsumerInvoker build(ConsumerPoller consumerPoller, String groupId) {
        final MethodInvoker methodInvoker = buildMethodInvoker(groupId);

        if (orderGuarantee) {
            final ExecutorStrategy executorStrategy = Optional.ofNullable(orderedProcessingStrategyBuilder)
                    .orElse(new OrderedProcessingStrategyBuilder(this))
                    .build(groupId);
            return new SimpleConsumerInvoker(methodInvoker, executorStrategy);
        }

        final ExecutorStrategy executorStrategy = Optional.ofNullable(unorderedProcessingStrategyBuilder)
                .orElse(new UnorderedProcessingStrategyBuilder(this))
                .build(groupId);

        final PauseAndRetryRejectionHandler rejectionHandler = new PauseAndRetryRejectionHandler(consumerPoller,
                executorStrategy);
        return new RejectionAwareConsumerInvoker(methodInvoker, executorStrategy, rejectionHandler);
    }
}
