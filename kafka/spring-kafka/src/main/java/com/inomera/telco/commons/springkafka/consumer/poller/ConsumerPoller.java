package com.inomera.telco.commons.springkafka.consumer.poller;

import com.inomera.telco.commons.springkafka.consumer.KafkaMessageConsumer;
import org.apache.kafka.common.TopicPartition;

/**
 * @author Serdar Kuzucu
 */
public interface ConsumerPoller {
    void start(KafkaMessageConsumer kafkaMessageConsumer);

    void stop();

    void setConsumerRecordHandler(ConsumerRecordHandler handler);

    void pause(TopicPartition topicPartition);
}
