package com.inomera.telco.commons.springkafka.consumer;

/**
 * @author Ramazan Karakaya
 * @author Turgay Can
 * @author Serdar Kuzucu
 * <p>
 * Defines the Kafka message acknowledgment (ack/commit) strategies based on processing mode.
 * These strategies determine when messages are acknowledged to the Kafka broker.
 */
public enum OffsetCommitStrategy {

    /**
     * **At-Least-Once Bulk Processing**
     * Messages are processed in bulk per partition, and acknowledgment is sent to Kafka **after** processing.
     * This ensures that messages are not lost, but may result in duplicate processing if a failure occurs before acknowledgment.
     */
    AT_LEAST_ONCE_BULK,

    /**
     * **At-Least-Once Single Processing**
     * Each message is processed individually per partition, and acknowledgment is sent to Kafka **after** processing.
     * Guarantees message delivery but may lead to duplicate processing in case of failures.
     */
    AT_LEAST_ONCE_SINGLE,

    /**
     * **At-Most-Once Bulk Processing**
     * Messages are processed in bulk per partition, and acknowledgment is sent to Kafka **before** processing.
     * Ensures that messages are processed at most once, but may result in data loss if a failure occurs during processing.
     */
    AT_MOST_ONCE_BULK,

    /**
     * **At-Most-Once Single Processing**
     * Each message is processed individually per partition, and acknowledgment is sent to Kafka **before** processing.
     * Guarantees that messages are not reprocessed but may lead to data loss if processing fails.
     */
    AT_MOST_ONCE_SINGLE
}