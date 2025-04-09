package com.inomera.telco.commons.springkafka.builder.virtual;

import com.inomera.telco.commons.springkafka.consumer.executor.virtual.VirtualExecutorStrategy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * @author Turgay Can
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class FixedInstanceVirtualExecutorStrategyBuilder implements VirtualExecutorStrategyBuilder {
    private final VirtualExecutorStrategy fixedInstance;

    @Override
    public VirtualExecutorStrategy build(String groupId) {
        return fixedInstance;
    }
}
