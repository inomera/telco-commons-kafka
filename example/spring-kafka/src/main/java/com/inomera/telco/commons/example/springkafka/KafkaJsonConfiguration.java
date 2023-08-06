package com.inomera.telco.commons.example.springkafka;

import com.inomera.telco.commons.example.springkafka.msg.DomainConstants;
import com.inomera.telco.commons.kafkajson.ClassIdRegistry;
import com.inomera.telco.commons.kafkajson.ImmutableClassIdRegistry;
import com.inomera.telco.commons.kafkajson.JsonKafkaDeserializer;
import com.inomera.telco.commons.kafkajson.JsonKafkaSerializer;
import com.inomera.telco.commons.springkafka.builder.KafkaConsumerBuilder;
import com.inomera.telco.commons.springkafka.consumer.ConsumerThreadStore;
import com.inomera.telco.commons.springkafka.consumer.KafkaMessageConsumer;
import com.inomera.telco.commons.springkafka.producer.KafkaMessagePublisher;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

@Configuration
public class KafkaJsonConfiguration {

    @Bean
    public JsonKafkaSerializer jsonKafkaSerializer() {
	return new JsonKafkaSerializer(new ImmutableClassIdRegistry(DomainConstants.CLASS_IDS));
    }

    @Bean
    public JsonKafkaDeserializer jsonKafkaDeserializer() {
	return new JsonKafkaDeserializer(new ImmutableClassIdRegistry(DomainConstants.CLASS_IDS));
    }

    @Bean("jsonKafkaProducerConfigurationProperties")
    @ConfigurationProperties("kafka-producers.json")
    public KafkaProducerConfigurationProperties jsonKafkaProducerConfigurationProperties() {
	return new KafkaProducerConfigurationProperties();
    }

    @Bean("jsonKafkaConsumerConfigurationProperties")
    @ConfigurationProperties(prefix = "kafka-consumers.json")
    public KafkaConsumerConfigurationProperties jsonKafkaConsumerConfigurationProperties() {
	return new KafkaConsumerConfigurationProperties();
    }

    @Bean
    public KafkaMessageConsumer jsonConsumer(KafkaConsumerBuilder builder,
					 KafkaConsumerConfigurationProperties jsonKafkaConsumerConfigurationProperties,
					 ConsumerThreadStore consumerThreadStore) {

	return builder.properties(jsonKafkaConsumerConfigurationProperties.getProperties())
		.groupId("json-logger")
		.topics("user-events")
		.offsetCommitStrategy(jsonKafkaConsumerConfigurationProperties.getOffsetCommitStrategy())
		.valueDeserializer(jsonKafkaDeserializer())
		.autoPartitionPause(true)
		.invoker()
		.unordered()
		.dynamicNamedExecutors()
		.configureExecutor("user-events", 1, 3, 1, TimeUnit.MINUTES)
.and()
		.and()
		.and()
		.threadStore(consumerThreadStore)
		.build();
    }

    @Bean("jsonKafkaMessagePublisher")
    public KafkaMessagePublisher<Serializable> jsonKafkaMessagePublisher(
	    KafkaProducerConfigurationProperties jsonKafkaProducerConfigurationProperties) {
	return new KafkaMessagePublisher(jsonKafkaSerializer(), jsonKafkaProducerConfigurationProperties.getProperties());
    }
}
