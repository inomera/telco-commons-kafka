package com.inomera.telco.commons.springkafka.consumer.executor;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Serdar Kuzucu
 * @author Turgay Can
 * Implementations of the interface use underlying OS threads.
 */
public interface ExecutorStrategy {
    ThreadPoolExecutor get(ConsumerRecord<String, ?> record);

    void start();

    void stop();
}
