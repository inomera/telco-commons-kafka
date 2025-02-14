package com.inomera.telco.commons.example.springkafka;

import com.google.protobuf.GeneratedMessage;
import com.inomera.telco.commons.example.domain.constant.KafkaTopicUtils;
import com.inomera.telco.commons.kafkaprotobuf.ImmutableClassIdRegistry;
import com.inomera.telco.commons.kafkaprotobuf.KafkaProtobufDeserializer;
import com.inomera.telco.commons.kafkaprotobuf.KafkaProtobufSerializer;
import com.inomera.telco.commons.springkafka.annotation.EnableKafkaListeners;
import com.inomera.telco.commons.springkafka.builder.KafkaConsumerBuilder;
import com.inomera.telco.commons.springkafka.consumer.*;
import com.inomera.telco.commons.springkafka.producer.KafkaMessagePublisher;
import com.inomera.telco.commons.springkafka.producer.KafkaTransactionalMessagePublisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import player.command.PlayerCreateCommandProto;
import player.event.PlayerNotificationEventProto;
import todo.command.TodoUpdateCommandProto;
import todo.event.TodoInfoRequestEventProto;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.inomera.telco.commons.example.domain.constant.DomainConstants.CLASS_IDS;
import static com.inomera.telco.commons.example.domain.constant.KafkaTopicConstants.TOPIC_PLAYER_CREATE_COMMAND;
import static com.inomera.telco.commons.example.domain.constant.KafkaTopicConstants.TOPIC_PLAYER_NOTIFICATION_EVENT;
import static org.apache.kafka.clients.producer.ProducerConfig.TRANSACTIONAL_ID_CONFIG;

/**
 * @author Serdar Kuzucu
 */
@SpringBootApplication
@EnableKafkaListeners
@EnableScheduling
public class SpringKafkaProtobufExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringKafkaProtobufExampleApplication.class, args);
    }

    @Bean
    public KafkaProtobufSerializer kafkaSerializer() {
        return new KafkaProtobufSerializer(new ImmutableClassIdRegistry(CLASS_IDS));
    }

    @Bean
    public KafkaProtobufDeserializer kafkaDeserializer() {
        return new KafkaProtobufDeserializer(new ImmutableClassIdRegistry(CLASS_IDS));
    }

    @Bean
    public ConsumerThreadStore consumerThreadStore() {
        return new PollerThreadStore();
    }

    @Bean(destroyMethod = "close")
    public ThreadStateChecker consumerThreadStateChecker(KafkaConsumerConfigurationProperties defaultKafkaConsumerConfigurationProperties) {
        return new PollerThreadStateChecker(consumerThreadStore(), pollerThreadNotifier(), defaultKafkaConsumerConfigurationProperties.getPollerThreadProperties());
    }

    @Bean
    public PollerThreadNotifier pollerThreadNotifier() {
        return new DefaultPollerThreadNotifier();
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
    @ConfigurationProperties(prefix = "kafka-consumers.retry")
    public KafkaConsumerConfigurationProperties retryKafkaConsumerConfigurationProperties() {
        return new KafkaConsumerConfigurationProperties();
    }

    @Bean
    public KafkaMessageConsumer consumer(KafkaConsumerBuilder builder,
                                         KafkaConsumerConfigurationProperties defaultKafkaConsumerConfigurationProperties,
                                         KafkaProtobufDeserializer kafkaDeserializer) {

        int threads = defaultKafkaConsumerConfigurationProperties.getNumberOfInvokerThreads();
        return builder.properties(defaultKafkaConsumerConfigurationProperties.getProperties())
                .groupId("event-logger")
                .topics(KafkaTopicUtils.getTopicNames(
                        PlayerCreateCommandProto.class,
                        PlayerNotificationEventProto.class,
                        TodoUpdateCommandProto.class,
                        TodoInfoRequestEventProto.class
                ))
                .offsetCommitStrategy(defaultKafkaConsumerConfigurationProperties.getOffsetCommitStrategy())
                .valueDeserializer(kafkaDeserializer)
                .autoPartitionPause(true)
                .invoker()
                .unordered()
                .dynamicNamedExecutors()
                .configureExecutor(TOPIC_PLAYER_CREATE_COMMAND, threads, threads, 3, TimeUnit.MINUTES)
                .configureExecutor(TOPIC_PLAYER_NOTIFICATION_EVENT, threads, threads, 3, TimeUnit.MINUTES)
                .queueCapacity(100_000)
//                .configureExecutor("example.unlistened-topic", 3, 5, 1, TimeUnit.MINUTES)
                .and()
                .and()
                .and()
                .threadStore(consumerThreadStore())
                .buildBulk();
    }

    @Bean
    public EventPublisher eventPublisher(KafkaMessagePublisher<? super GeneratedMessage> kafkaPublisher) {
        return new EventPublisher(kafkaPublisher);
    }

    @Bean
    public TransactionalEventPublisher transactionalEventPublisher(KafkaTransactionalMessagePublisher<? super GeneratedMessage> transactionalKafkaPublisher) {
        return new TransactionalEventPublisher(transactionalKafkaPublisher);
    }

    @Bean
    public KafkaMessagePublisher<? super GeneratedMessage> kafkaPublisher(
            KafkaProducerConfigurationProperties defaultKafkaProducerConfigurationProperties,
            KafkaProtobufSerializer kafkaSerializer) {
        return new KafkaMessagePublisher<>(kafkaSerializer, defaultKafkaProducerConfigurationProperties.getProperties());
    }

    @Bean
    public KafkaTransactionalMessagePublisher<? super GeneratedMessage> transactionalKafkaPublisher(
            KafkaProducerConfigurationProperties defaultKafkaProducerConfigurationProperties,
            KafkaProtobufSerializer kafkaSerializer) {
        Properties properties = defaultKafkaProducerConfigurationProperties.getProperties();
        properties.put(TRANSACTIONAL_ID_CONFIG, "spring-kafka-protobuf-");
        return new KafkaTransactionalMessagePublisher<>(kafkaSerializer, defaultKafkaProducerConfigurationProperties.getProperties());
    }
}
