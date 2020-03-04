package com.inomera.telco.commons.springkafka.consumer;

import org.springframework.context.SmartLifecycle;

/**
 * @author Serdar Kuzucu
 */
public interface KafkaMessageConsumer extends SmartLifecycle {
    void start();

    void stop();
}
