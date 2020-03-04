package com.inomera.telco.commons.springkafka.consumer.poller;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.concurrent.Future;

/**
 * @author Serdar Kuzucu
 */
public interface ConsumerRecordHandler {
    Future<ConsumerRecord<String, ?>> handle(ConsumerRecord<String, ?> record);
}
