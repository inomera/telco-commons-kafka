package com.inomera.telco.commons.springkafka.producer;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.lang.Nullable;

import java.time.Duration;

/**
 * The strategy to produce a {@link Producer} instance(s).
 *
 * @param <V> the value type.
 */
public interface ProducerFactory<V> {

    /**
     * The default close timeout duration as 30 seconds.
     */
    Duration DEFAULT_PHYSICAL_CLOSE_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Create a producer which will be transactional if the factory is so configured.
     *
     * @return the producer.
     * @see #transactionCapable()
     */
    Producer<String, V> createProducer();

    /**
     * Create a producer with an overridden transaction id prefix.
     *
     * @param txIdPrefix the transaction id prefix.
     * @return the producer.
     */
    default Producer<String, V> createProducer(@Nullable @SuppressWarnings("unused") String txIdPrefix) {
        throw new UnsupportedOperationException("This factory does not support this method");
    }

    /**
     * Return true if the factory supports transactions.
     *
     * @return true if transactional.
     */
    default boolean transactionCapable() {
        return false;
    }

    /**
     * Return the transaction id prefix.
     *
     * @return the prefix or null if not configured.
     */
    @Nullable
    default String getTransactionIdPrefix() {
        return null;
    }

    /**
     * Get the physical close timeout.
     *
     * @return the timeout.
     */
    default Duration getPhysicalCloseTimeout() {
        return DEFAULT_PHYSICAL_CLOSE_TIMEOUT;
    }

    /**
     * Return the configured key serializer (if provided as an object instead
     * of a class name in the properties).
     *
     * @return the serializer.
     */
    @Nullable
    default Serializer<String> getKeySerializer() {
        return null;
    }

    /**
     * Return the configured value serializer (if provided as an object instead
     * of a class name in the properties).
     *
     * @return the serializer.
     */
    @Nullable
    default Serializer<V> getValueSerializer() {
        return null;
    }

    /**
     * Reset any state in the factory, if supported.
     */
    default void reset() {
    }
}
