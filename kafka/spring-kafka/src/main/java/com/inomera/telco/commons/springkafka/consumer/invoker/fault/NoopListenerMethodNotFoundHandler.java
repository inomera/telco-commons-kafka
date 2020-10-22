package com.inomera.telco.commons.springkafka.consumer.invoker.fault;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * @author Serdar Kuzucu
 */
public class NoopListenerMethodNotFoundHandler implements ListenerMethodNotFoundHandler {
    @Override
    public void onListenerMethodNotFound(String groupId, ConsumerRecord<String, ?> record) {
        // No operations, really.
    }
}
