package com.inomera.telco.commons.springkafka.consumer.executor.virtual;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.concurrent.ExecutorService;

/**
 * @author Turgay Can
 * Implementations of the interface use underlying Virtual threads.
 */
public interface ParameterBasedVirtualExecutorStrategy extends VirtualExecutorStrategy {

    ExecutorService get(ConsumerRecord<String, ?> record);

}
