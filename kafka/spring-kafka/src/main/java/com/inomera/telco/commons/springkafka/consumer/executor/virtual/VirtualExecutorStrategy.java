package com.inomera.telco.commons.springkafka.consumer.executor.virtual;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.concurrent.ExecutorService;

/**
 * @author Turgay Can
 * Implementations of the interface use underlying Virtual threads.
 */
public interface VirtualExecutorStrategy {

    void start();

    void stop();

    default ExecutorService getExecutor(ConsumerRecord<String, ?> record) {
        if (this instanceof ParameterBasedVirtualExecutorStrategy) {
            return ((ParameterBasedVirtualExecutorStrategy) this).get(record);
        }
        return ((DefaultVirtualExecutorStrategy) this).get();
    }
}
