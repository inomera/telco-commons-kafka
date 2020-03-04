package com.inomera.telco.commons.springkafka.consumer.poller;

import org.apache.kafka.common.TopicPartition;

/**
 * @author Serdar Kuzucu
 */
public interface ConsumerPoller {
    void start();

    void stop();

    void setConsumerRecordHandler(ConsumerRecordHandler handler);

    void pause(TopicPartition topicPartition);
}
