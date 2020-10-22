package com.inomera.telco.commons.springkafka.consumer.invoker.fault;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * @author Serdar Kuzucu
 */
public interface ListenerMethodNotFoundHandler {
    void onListenerMethodNotFound(String groupId, ConsumerRecord<String, ?> record);
}
