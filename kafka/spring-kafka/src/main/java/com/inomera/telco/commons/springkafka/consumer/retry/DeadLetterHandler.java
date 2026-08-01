package com.inomera.telco.commons.springkafka.consumer.retry;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Collection;

/**
 * Invoked when a record has exhausted its retries. Previously such records were logged and
 * dropped with no hook at all.
 *
 * @author Turgay Can
 */
public interface DeadLetterHandler {

    void onExhausted(ConsumerRecord<String, ?> record, int attempts);

    default void onExhausted(Collection<ConsumerRecord<String, ?>> records, int attempts) {
        records.forEach(record -> onExhausted(record, attempts));
    }
}
