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
        if (this instanceof ParameterBasedVirtualExecutorStrategy parameterBased) {
            return parameterBased.get(record);
        }
        if (this instanceof DefaultVirtualExecutorStrategy defaultBased) {
            return defaultBased.get();
        }
        // Used to be an unchecked cast, so a third implementation failed with a bare
        // ClassCastException on the hot path.
        throw new IllegalStateException(getClass().getName() + " must implement either "
                + ParameterBasedVirtualExecutorStrategy.class.getSimpleName() + " or "
                + DefaultVirtualExecutorStrategy.class.getSimpleName());
    }
}
