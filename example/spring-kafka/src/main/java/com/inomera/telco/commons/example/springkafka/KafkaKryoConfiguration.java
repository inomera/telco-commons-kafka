package com.inomera.telco.commons.example.springkafka;

import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.inomera.telco.commons.example.springkafka.msg.AbstractMessage;
import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedBeautifullyMessage;
import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedMessage;
import com.inomera.telco.commons.example.springkafka.msg.UnListenedMessage;
import com.inomera.telco.commons.kafkakryo.KryoClassRegistry;
import com.inomera.telco.commons.kafkakryo.KryoFactory;
import com.inomera.telco.commons.kafkakryo.KryoKafkaDeserializer;
import com.inomera.telco.commons.kafkakryo.KryoKafkaSerializer;
import com.inomera.telco.commons.kafkakryo.ThreadLocalKryoFactory;
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
public class KafkaKryoConfiguration {

    @Bean
    public KryoClassRegistry kryoClassRegistry() {
	return kryo -> {
	    kryo.register(SomethingHappenedMessage.class, new JavaSerializer(), 1001);
	    kryo.register(SomethingHappenedBeautifullyMessage.class, new JavaSerializer(), 1002);
	    kryo.register(AbstractMessage.class, new JavaSerializer(), 1002);
	    kryo.register(UnListenedMessage.class, new JavaSerializer(), 1003);
	};
    }

    @Bean
    public KryoFactory kryoFactory() {
	return new ThreadLocalKryoFactory(kryoClassRegistry());
    }

    @Bean
    public KryoKafkaSerializer kafkaSerializer() {
	return new KryoKafkaSerializer(kryoFactory());
    }

    @Bean
    public KryoKafkaDeserializer kafkaDeserializer() {
	return new KryoKafkaDeserializer(kryoFactory());
    }

    @Bean
    @ConfigurationProperties("kafka-producers.default")
    public KafkaProducerConfigurationProperties defaultKafkaProducerConfigurationProperties() {
	return new KafkaProducerConfigurationProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "kafka-consumers.default")
    public KafkaConsumerConfigurationProperties defaultKafkaConsumerConfigurationProperties() {
	return new KafkaConsumerConfigurationProperties();
    }

    @Bean
    public KafkaMessageConsumer consumer(KafkaConsumerBuilder builder,
					 KafkaConsumerConfigurationProperties defaultKafkaConsumerConfigurationProperties,
					 ConsumerThreadStore consumerThreadStore) {

	return builder.properties(defaultKafkaConsumerConfigurationProperties.getProperties())
		.groupId("event-logger")
		.topics("mouse-event.click", "mouse-event.dblclick", "example.unlistened-topic")
		.offsetCommitStrategy(defaultKafkaConsumerConfigurationProperties.getOffsetCommitStrategy())
		.valueDeserializer(kafkaDeserializer())
		.autoPartitionPause(true)
		.invoker()
		.interceptor(new KafkaMdcInterceptor())
		.unordered()
		.dynamicNamedExecutors()
		.configureExecutor("mouse-event.click", 3, 5, 1, TimeUnit.MINUTES)
		.configureExecutor("mouse-event.dblclick", 3, 5, 1, TimeUnit.MINUTES)
		.configureExecutor("example.unlistened-topic", 3, 5, 1, TimeUnit.MINUTES)
		.and()
		.and()
		.and()
		.threadStore(consumerThreadStore)
		.build();
    }

    @Bean("bulkConsumer")
    public KafkaMessageConsumer bulkConsumer(KafkaConsumerBuilder builder,
					     KafkaConsumerConfigurationProperties defaultKafkaConsumerConfigurationProperties,
					     ConsumerThreadStore consumerThreadStore) {

	return builder.properties(defaultKafkaConsumerConfigurationProperties.getProperties())
		.groupId("bulk-event-logger")
		.topics("mouse-bulk-event.click", "mouse-bulk-event.dblclick", "bulk-example.unlistened-topic")
		.offsetCommitStrategy(defaultKafkaConsumerConfigurationProperties.getOffsetCommitStrategy())
		.valueDeserializer(kafkaDeserializer())
		.autoPartitionPause(true)
		.invoker()
		.interceptor(new KafkaMdcInterceptor())
		.unordered()
		.dynamicNamedExecutors()
		.configureExecutor("mouse-bulk-event.click", 3, 5, 1, TimeUnit.MINUTES)
		.configureExecutor("mouse-bulk-event.dblclick", 3, 5, 1, TimeUnit.MINUTES)
		.configureExecutor("bulk-example.unlistened-topic", 3, 5, 1, TimeUnit.MINUTES)
		.and()
		.and()
		.and()
		.threadStore(consumerThreadStore)
		.buildBulk();
    }

    @Bean("bulkRetryConsumer")
    public KafkaMessageConsumer bulkRetryConsumer(KafkaConsumerBuilder builder,				  KafkaConsumerConfigurationProperties defaultKafkaConsumerConfigurationProperties,
						  ConsumerThreadStore consumerThreadStore) {

	return builder.properties(defaultKafkaConsumerConfigurationProperties.getProperties())
		.groupId("retry-bulk-event-logger")
		.topics("mouse-bulk-event.click", "mouse-bulk-event.dblclick", "bulk-example.unlistened-topic")
		.offsetCommitStrategy(defaultKafkaConsumerConfigurationProperties.getOffsetCommitStrategy())
		.valueDeserializer(kafkaDeserializer())
		.autoPartitionPause(true)
		.invoker()
		.interceptor(new KafkaMdcInterceptor())
		.unordered()
		.dynamicNamedExecutors()
		.configureExecutor("mouse-bulk-event.click", 3, 5, 1, TimeUnit.MINUTES)
		.configureExecutor("mouse-bulk-event.dblclick", 3, 5, 1, TimeUnit.MINUTES)
		.configureExecutor("bulk-example.unlistened-topic", 3, 5, 1, TimeUnit.MINUTES)
		.and()
		.and()
		.and()
		.threadStore(consumerThreadStore)
		.buildBulk();
    }

    @Bean
    public KafkaMessagePublisher<Serializable> stringKafkaMessagePublisher(
	    KafkaProducerConfigurationProperties defaultKafkaProducerConfigurationProperties) {
	return new KafkaMessagePublisher<>(kafkaSerializer(), defaultKafkaProducerConfigurationProperties.getProperties());
    }
}
