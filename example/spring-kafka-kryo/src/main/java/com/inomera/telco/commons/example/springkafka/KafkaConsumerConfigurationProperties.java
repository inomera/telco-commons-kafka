package com.inomera.telco.commons.example.springkafka;

import com.inomera.telco.commons.springkafka.consumer.OffsetCommitStrategy;
import lombok.Getter;
import lombok.Setter;

import java.util.Properties;

@Getter
@Setter
public class KafkaConsumerConfigurationProperties {
    private Properties properties;
    private Properties pollerThreadProperties;
    private int numberOfInvokerThreads;
    private OffsetCommitStrategy offsetCommitStrategy = OffsetCommitStrategy.AT_MOST_ONCE_BULK;
}
