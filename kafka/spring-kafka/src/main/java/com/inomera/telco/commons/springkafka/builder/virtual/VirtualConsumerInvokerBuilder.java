package com.inomera.telco.commons.springkafka.builder.virtual;

import com.inomera.telco.commons.lang.Assert;
import com.inomera.telco.commons.springkafka.consumer.executor.virtual.VirtualExecutorStrategy;
import com.inomera.telco.commons.springkafka.consumer.invoker.*;
import com.inomera.telco.commons.springkafka.consumer.invoker.fault.ListenerMethodNotFoundHandler;
import com.inomera.telco.commons.springkafka.consumer.invoker.fault.LoggingListenerMethodNotFoundHandler;
import com.inomera.telco.commons.springkafka.consumer.invoker.virtual.*;
import com.inomera.telco.commons.springkafka.consumer.poller.ConsumerPoller;
import lombok.RequiredArgsConstructor;

import java.util.*;

/**
 * @author Turgay Can
 */
@RequiredArgsConstructor
public class VirtualConsumerInvokerBuilder {
    private final VirtualKafkaConsumerBuilder virtualKafkaConsumerBuilder;
    private final ListenerMethodRegistry listenerMethodRegistry;

    private final List<ListenerInvocationInterceptor> interceptors = new ArrayList<>();
    private boolean orderGuarantee = true;
    private ListenerMethodNotFoundHandler listenerMethodNotFoundHandler;

    private UnorderedVirtualProcessingStrategyBuilder unorderedVirtualProcessingStrategyBuilder;
    private OrderedProcessingVirtualStrategyBuilder orderedProcessingVirtualStrategyBuilder;

    public VirtualConsumerInvokerBuilder interceptor(ListenerInvocationInterceptor interceptor) {
        Assert.notNull(interceptor, "interceptor is null");
        this.interceptors.add(interceptor);
        return this;
    }

    public VirtualConsumerInvokerBuilder interceptors(Collection<ListenerInvocationInterceptor> interceptors) {
        Assert.notNull(interceptors, "interceptors is null");
        this.interceptors.addAll(interceptors);
        return this;
    }

    public OrderedProcessingVirtualStrategyBuilder ordered() {
        this.orderGuarantee = true;
        this.orderedProcessingVirtualStrategyBuilder = new OrderedProcessingVirtualStrategyBuilder(this);
        return orderedProcessingVirtualStrategyBuilder;
    }

    public UnorderedVirtualProcessingStrategyBuilder unordered() {
        this.orderGuarantee = false;
        this.unorderedVirtualProcessingStrategyBuilder = new UnorderedVirtualProcessingStrategyBuilder(this);
        return unorderedVirtualProcessingStrategyBuilder;
    }

    public VirtualConsumerInvokerBuilder listenerMethodNotFoundHandler(
            ListenerMethodNotFoundHandler listenerMethodNotFoundHandler) {
        Assert.notNull(listenerMethodNotFoundHandler, "listenerMethodNotFoundHandler is null");
        this.listenerMethodNotFoundHandler = listenerMethodNotFoundHandler;
        return this;
    }

    public VirtualKafkaConsumerBuilder and() {
        return virtualKafkaConsumerBuilder;
    }

    private ListenerMethodNotFoundHandler getOrCreateListenerMethodNotFoundHandler() {
        return Objects.requireNonNullElseGet(this.listenerMethodNotFoundHandler, LoggingListenerMethodNotFoundHandler::new);
    }

    private MethodInvoker buildMethodInvoker(String groupId) {
        return new MethodInvoker(groupId, listenerMethodRegistry, interceptors,
                getOrCreateListenerMethodNotFoundHandler());
    }

    ConsumerInvoker build(ConsumerPoller consumerPoller, String groupId) {
        final MethodInvoker methodInvoker = buildMethodInvoker(groupId);

        if (orderGuarantee) {
            final VirtualExecutorStrategy executorStrategy = Optional.ofNullable(orderedProcessingVirtualStrategyBuilder)
                    .orElse(new OrderedProcessingVirtualStrategyBuilder(this))
                    .build(groupId);
            return new VirtualSimpleConsumerInvoker(methodInvoker, executorStrategy);
        }

        final VirtualExecutorStrategy virtualExecutorStrategy = Optional.ofNullable(unorderedVirtualProcessingStrategyBuilder)
                .orElse(new UnorderedVirtualProcessingStrategyBuilder(this))
                .build(groupId);

        final RejectionHandler rejectionHandler = new VirtualPauseAndRetryRejectionHandler(consumerPoller,
                virtualExecutorStrategy);
        return new VirtualRejectionAwareConsumerInvoker(methodInvoker, virtualExecutorStrategy, rejectionHandler);
    }

    BulkConsumerInvoker buildBulk(ConsumerPoller consumerPoller, String groupId) {
        final MethodInvoker methodInvoker = buildMethodInvoker(groupId);

        if (orderGuarantee) {
            final VirtualExecutorStrategy executorStrategy = Optional.ofNullable(orderedProcessingVirtualStrategyBuilder)
                    .orElse(new OrderedProcessingVirtualStrategyBuilder(this))
                    .build(groupId);
            return new VirtualBulkSimpleConsumerInvoker(methodInvoker, executorStrategy);
        }

        final VirtualExecutorStrategy executorStrategy = Optional.ofNullable(unorderedVirtualProcessingStrategyBuilder)
                .orElse(new UnorderedVirtualProcessingStrategyBuilder(this))
                .build(groupId);

        final RejectionHandler rejectionHandler = new VirtualPauseAndRetryRejectionHandler(consumerPoller,
                executorStrategy);
        return new VirtualBulkRejectionAwareConsumerInvoker(methodInvoker, executorStrategy, rejectionHandler);
    }
}
