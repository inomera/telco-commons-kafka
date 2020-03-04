package com.inomera.telco.commons.springkafka.builder;

import com.inomera.telco.commons.springkafka.consumer.executor.ExecutorStrategy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * @author Serdar Kuzucu
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class FixedInstanceExecutorStrategyBuilder extends AbstractExecutorStrategyBuilder {
    private final ExecutorStrategy fixedInstance;

    @Override
    ExecutorStrategy build(String groupId) {
        return fixedInstance;
    }
}
