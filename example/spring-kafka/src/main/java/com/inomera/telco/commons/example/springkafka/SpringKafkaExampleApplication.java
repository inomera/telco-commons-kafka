package com.inomera.telco.commons.example.springkafka;

import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.inomera.telco.commons.example.springkafka.msg.AbstractMessage;
import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedBeautifullyMessage;
import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedMessage;
import com.inomera.telco.commons.kafkakryo.*;
import com.inomera.telco.commons.springkafka.annotation.EnableKafkaListeners;
import com.inomera.telco.commons.springkafka.consumer.KafkaConsumerBuilder;
import com.inomera.telco.commons.springkafka.consumer.KafkaMessageConsumer;
import com.inomera.telco.commons.springkafka.consumer.OffsetCommitStrategy;
import com.inomera.telco.commons.springkafka.producer.KafkaMessagePublisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Properties;

/**
 * @author Serdar Kuzucu
 */
@SpringBootApplication
@EnableKafkaListeners
@EnableScheduling
public class SpringKafkaExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringKafkaExampleApplication.class, args);
    }

    @Bean
    public KryoClassRegistry kryoClassRegistry() {
        return kryo -> {
            kryo.register(SomethingHappenedMessage.class, new JavaSerializer(), 1001);
            kryo.register(SomethingHappenedBeautifullyMessage.class, new JavaSerializer(), 1002);
            kryo.register(AbstractMessage.class, new JavaSerializer(), 1002);
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
    public KafkaMessageConsumer consumer(KafkaConsumerBuilder builder) throws IOException {
        final Properties properties = new Properties();
        properties.load(new StringReader("enable.auto.commit=false\n" +
                "auto.commit.interval.ms=2147483647\n" +
                "bootstrap.servers=localhost:9092\n" +
                "heartbeat.interval.ms=10000\n" +
                "request.timeout.ms=31000\n" +
                "session.timeout.ms=30000\n" +
                "max.partition.fetch.bytes=15728640\n" +
                "max.poll.records=100\n" +
                "metadata.max.age.ms=10000"));

        return builder.properties(properties)
                .groupId("event-saver")
                .topics("mouse-event.click")
                .offsetCommitStrategy(OffsetCommitStrategy.AT_MOST_ONCE_SINGLE)
                .valueDeserializer(kafkaDeserializer())
                .numberOfThreads(5)
                .build();
    }

    @Bean
    public KafkaMessageConsumer consumer2(KafkaConsumerBuilder builder) throws IOException {
        final Properties properties = new Properties();
        properties.load(new StringReader("enable.auto.commit=false\n" +
                "auto.commit.interval.ms=2147483647\n" +
                "bootstrap.servers=localhost:9092\n" +
                "heartbeat.interval.ms=10000\n" +
                "request.timeout.ms=31000\n" +
                "session.timeout.ms=30000\n" +
                "max.partition.fetch.bytes=15728640\n" +
                "max.poll.records=100\n" +
                "metadata.max.age.ms=10000"));

        return builder.properties(properties)
                .groupId("event-handler")
                .topics("mouse-event.click")
                .offsetCommitStrategy(OffsetCommitStrategy.AT_MOST_ONCE_SINGLE)
                .valueDeserializer(kafkaDeserializer())
                .numberOfThreads(5)
                .build();
    }

    @Bean
    public KafkaMessagePublisher<Serializable> stringKafkaMessagePublisher() throws IOException {
        final Properties properties = new Properties();
        properties.load(new StringReader("max.request.size=15728640\n" +
                "bootstrap.servers=localhost:9092\n" +
                "enable.idempotence=true\n" +
                "retries=2147483647\n" +
                "max.in.flight.requests.per.connection=1\n" +
                "acks=all\n" +
                "linger.ms=10\n" +
                "metadata.max.age.ms=10000"));

        return new KafkaMessagePublisher<>(kafkaSerializer(), properties);
    }
}
