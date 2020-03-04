package com.inomera.telco.commons.springkafka.consumer.invoker;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.concurrent.Future;

/**
 * @author Serdar Kuzucu
 */
public interface ConsumerInvoker {
    void start();

    void stop();

    Future<ConsumerRecord<String, ?>> invoke(ConsumerRecord<String, ?> record);
}
