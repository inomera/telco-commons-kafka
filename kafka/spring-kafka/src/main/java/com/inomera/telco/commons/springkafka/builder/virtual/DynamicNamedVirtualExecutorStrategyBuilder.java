package com.inomera.telco.commons.springkafka.builder.virtual;

import com.inomera.telco.commons.springkafka.consumer.executor.virtual.DynamicNamedVirtualExecutorStrategy;
import com.inomera.telco.commons.springkafka.consumer.executor.virtual.VirtualExecutorStrategy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.*;
import java.util.function.Function;

/**
 * @author Turgay Can
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class DynamicNamedVirtualExecutorStrategyBuilder implements VirtualExecutorStrategyBuilder {
    private final Set<String> executors = new LinkedHashSet<>();

    private final UnorderedVirtualProcessingStrategyBuilder unorderedVirtualProcessingStrategyBuilder;

    private Function<ConsumerRecord<String, ?>, String> executorNamingFunction;

    public DynamicNamedVirtualExecutorStrategyBuilder executorName(String name) {
        executors.add(name);
        return this;
    }

    public DynamicNamedVirtualExecutorStrategyBuilder executorNamingFunction(Function<ConsumerRecord<String, ?>, String> executorNamingFunction) {
        this.executorNamingFunction = executorNamingFunction;
        return this;
    }

    public UnorderedVirtualProcessingStrategyBuilder and() {
        return this.unorderedVirtualProcessingStrategyBuilder;
    }

    @Override
    public VirtualExecutorStrategy build(String groupId) {
        DynamicNamedVirtualExecutorStrategy dynamicNamedVirtualExecutorStrategy = new DynamicNamedVirtualExecutorStrategy(getExecutorNamingFunction());
        executors.forEach(dynamicNamedVirtualExecutorStrategy::getOrCreateByExecutorName);
        return dynamicNamedVirtualExecutorStrategy;
    }

    private Function<ConsumerRecord<String, ?>, String> getExecutorNamingFunction() {
        return Objects.requireNonNullElseGet(executorNamingFunction, () -> ConsumerRecord::topic);
    }

}
