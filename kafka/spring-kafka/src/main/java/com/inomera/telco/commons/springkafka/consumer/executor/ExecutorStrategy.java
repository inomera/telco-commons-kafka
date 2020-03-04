package com.inomera.telco.commons.springkafka.consumer.executor;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Serdar Kuzucu
 */
public interface ExecutorStrategy {
    ThreadPoolExecutor get(ConsumerRecord<String, ?> record);

    void start();

    void stop();
}
