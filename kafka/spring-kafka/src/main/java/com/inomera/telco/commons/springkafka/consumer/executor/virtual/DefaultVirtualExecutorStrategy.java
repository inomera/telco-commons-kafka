package com.inomera.telco.commons.springkafka.consumer.executor.virtual;

import java.util.concurrent.ExecutorService;

/**
 * @author Turgay Can
 * Implementations of the interface use underlying Virtual threads.
 */
public interface DefaultVirtualExecutorStrategy extends VirtualExecutorStrategy {

    ExecutorService get();

}
