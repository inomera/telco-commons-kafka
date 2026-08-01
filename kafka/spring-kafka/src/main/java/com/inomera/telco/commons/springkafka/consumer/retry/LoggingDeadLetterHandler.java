package com.inomera.telco.commons.springkafka.consumer.retry;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link DeadLetterHandler}: logs coordinates only, never the payload.
 *
 * @author Turgay Can
 */
public class LoggingDeadLetterHandler implements DeadLetterHandler {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingDeadLetterHandler.class);

    @Override
    public void onExhausted(ConsumerRecord<String, ?> record, int attempts) {
        LOG.warn("Retries exhausted after {} attempts, dropping {}-{}@{} (key={})",
                attempts, record.topic(), record.partition(), record.offset(), record.key());
    }
}
