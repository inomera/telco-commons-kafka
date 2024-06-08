package com.inomera.telco.commons.example.springkafka;

import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.inomera.telco.commons.example.springkafka.msg.*;
import com.inomera.telco.commons.kafkakryo.*;
import com.inomera.telco.commons.springkafka.annotation.EnableKafkaListeners;
import com.inomera.telco.commons.springkafka.builder.KafkaConsumerBuilder;
import com.inomera.telco.commons.springkafka.consumer.*;
import com.inomera.telco.commons.springkafka.producer.KafkaTransactionalMessagePublisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.kafka.clients.producer.ProducerConfig.TRANSACTIONAL_ID_CONFIG;

/**
 * @author Serdar Kuzucu
 */
@SpringBootApplication
@EnableKafkaListeners
@EnableScheduling
public class SpringKafkaKryoExampleApplication implements SchedulingConfigurer {
    public static void main(String[] args) {
        SpringApplication.run(SpringKafkaKryoExampleApplication.class, args);
    }

    @Bean
    public KryoClassRegistry kryoClassRegistry() {
        return kryo -> {
            kryo.register(SomethingHappenedMessage.class, new JavaSerializer(), 1001);
            kryo.register(SomethingHappenedBeautifullyMessage.class, new JavaSerializer(), 1002);
            kryo.register(AbstractMessage.class, new JavaSerializer(), 1003);
            kryo.register(UnListenedMessage.class, new JavaSerializer(), 1004);
            kryo.register(SomethingHappenedConsumerMessage.class, new JavaSerializer(), 1005);
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
    public ConsumerThreadStore consumerThreadStore() {
        return new PollerThreadStore();
    }

    @Bean
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
                                         KafkaConsumerConfigurationProperties defaultKafkaConsumerConfigurationProperties) {

        return builder.properties(defaultKafkaConsumerConfigurationProperties.getProperties())
                .groupId("event-logger")
                .topics("mouse-event.click", "mouse-event.dblclick", "example.unlistened-topic")
                .offsetCommitStrategy(defaultKafkaConsumerConfigurationProperties.getOffsetCommitStrategy())
                .valueDeserializer(kafkaDeserializer())
                .autoPartitionPause(true)
                .invoker()
                .unordered()
                .dynamicNamedExecutors()
                .configureExecutor("mouse-event.click", 3, 5, 1, TimeUnit.MINUTES)
                .configureExecutor("mouse-event.dblclick", 3, 5, 1, TimeUnit.MINUTES)
                .configureExecutor("example.unlistened-topic", 3, 5, 1, TimeUnit.MINUTES)
                .and()
                .and()
                .and()
                .threadStore(consumerThreadStore())
                .build();
    }

    @Bean("bulkConsumer")
    public KafkaMessageConsumer bulkConsumer(KafkaConsumerBuilder builder,
                                             KafkaConsumerConfigurationProperties defaultKafkaConsumerConfigurationProperties) {

        return builder.properties(defaultKafkaConsumerConfigurationProperties.getProperties())
                .groupId("bulk-event-logger")
                .topics("mouse-bulk-event.click", "mouse-bulk-event.dblclick", "bulk-example.unlistened-topic")
                .offsetCommitStrategy(defaultKafkaConsumerConfigurationProperties.getOffsetCommitStrategy())
                .valueDeserializer(kafkaDeserializer())
                .autoPartitionPause(true)
                .invoker()
                .unordered()
                .dynamicNamedExecutors()
                .configureExecutor("mouse-bulk-event.click", 3, 5, 1, TimeUnit.MINUTES)
                .configureExecutor("mouse-bulk-event.dblclick", 3, 5, 1, TimeUnit.MINUTES)
                .configureExecutor("bulk-example.unlistened-topic", 3, 5, 1, TimeUnit.MINUTES)
                .and()
                .and()
                .and()
                .threadStore(consumerThreadStore())
                .buildBulk();
    }

    @Bean("bulkRetryConsumer")
    public KafkaMessageConsumer bulkRetryConsumer(KafkaConsumerBuilder builder,
                                                  KafkaConsumerConfigurationProperties defaultKafkaConsumerConfigurationProperties) {

        return builder.properties(defaultKafkaConsumerConfigurationProperties.getProperties())
                .groupId("retry-bulk-event-logger")
                .topics("mouse-bulk-event.click", "mouse-bulk-event.dblclick", "bulk-example.unlistened-topic")
                .offsetCommitStrategy(defaultKafkaConsumerConfigurationProperties.getOffsetCommitStrategy())
                .valueDeserializer(kafkaDeserializer())
                .autoPartitionPause(true)
                .invoker()
                .unordered()
                .dynamicNamedExecutors()
                .configureExecutor("mouse-bulk-event.click", 3, 5, 1, TimeUnit.MINUTES)
                .configureExecutor("mouse-bulk-event.dblclick", 3, 5, 1, TimeUnit.MINUTES)
                .configureExecutor("bulk-example.unlistened-topic", 3, 5, 1, TimeUnit.MINUTES)
                .and()
                .and()
                .and()
                .threadStore(consumerThreadStore())
                .buildBulk();
    }

    @Bean
    public KafkaTransactionalMessagePublisher<Serializable> stringKafkaMessagePublisher(
            KafkaProducerConfigurationProperties defaultKafkaProducerConfigurationProperties) {
        Properties properties = defaultKafkaProducerConfigurationProperties.getProperties();
        properties.put(TRANSACTIONAL_ID_CONFIG, "spring-kafka-kryo-");
        return new KafkaTransactionalMessagePublisher<>(kafkaSerializer(), properties);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor());
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService taskExecutor() {
        return Executors.newScheduledThreadPool(16);
    }
}
