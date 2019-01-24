package com.inomera.telco.commons.springkafka.configuration;

import com.inomera.telco.commons.springkafka.consumer.KafkaConsumerBuilder;
import com.inomera.telco.commons.springkafka.consumer.ListenerMethodRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * @author Serdar Kuzucu
 */
@Configuration
public class KafkaBootstrapConfiguration {
    @Bean
    @Scope("prototype")
    public KafkaConsumerBuilder kafkaConsumerBuilder(ListenerMethodRegistry listenerMethodRegistry) {
        return KafkaConsumerBuilder.builder(listenerMethodRegistry);
    }
}
