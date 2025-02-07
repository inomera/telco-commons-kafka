package com.inomera.telco.commons.example.springkafka;

import lombok.Getter;
import lombok.Setter;

import java.util.Properties;

@Getter
@Setter
public class KafkaProducerConfigurationProperties {
    private Properties properties;
}
