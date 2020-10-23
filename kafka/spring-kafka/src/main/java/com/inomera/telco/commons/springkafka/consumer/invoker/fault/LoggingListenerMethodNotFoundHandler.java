package com.inomera.telco.commons.springkafka.consumer.invoker.fault;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Serdar Kuzucu
 */
public class LoggingListenerMethodNotFoundHandler implements ListenerMethodNotFoundHandler {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingListenerMethodNotFoundHandler.class);

    @Override
    public void onListenerMethodNotFound(String groupId, ConsumerRecord<String, ?> record) {
        LOG.info("Listener method not found for topic={}, group={}, key={}, value={}",
                record.topic(), groupId, record.key(), record.value());
    }
}
