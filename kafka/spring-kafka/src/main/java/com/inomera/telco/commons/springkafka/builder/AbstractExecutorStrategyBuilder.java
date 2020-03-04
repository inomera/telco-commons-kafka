package com.inomera.telco.commons.springkafka.builder;

import com.inomera.telco.commons.springkafka.consumer.executor.ExecutorStrategy;

/**
 * @author Serdar Kuzucu
 */
public abstract class AbstractExecutorStrategyBuilder {
    abstract ExecutorStrategy build(String groupId);
}
