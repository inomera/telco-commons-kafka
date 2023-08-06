package com.inomera.telco.commons.example.springkafka;

import com.inomera.telco.commons.springkafka.annotation.EnableKafkaListeners;
import com.inomera.telco.commons.springkafka.consumer.ConsumerThreadStore;
import com.inomera.telco.commons.springkafka.consumer.DefaultPollerThreadNotifier;
import com.inomera.telco.commons.springkafka.consumer.PollerThreadNotifier;
import com.inomera.telco.commons.springkafka.consumer.PollerThreadStateChecker;
import com.inomera.telco.commons.springkafka.consumer.PollerThreadStore;
import com.inomera.telco.commons.springkafka.consumer.ThreadStateChecker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

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
    public ConsumerThreadStore consumerThreadStore() {
        return new PollerThreadStore();
    }

    @Bean
    public ThreadStateChecker consumerThreadStateChecker(KafkaConsumerConfigurationProperties defaultKafkaConsumerConfigurationProperties) {
        return new PollerThreadStateChecker(consumerThreadStore(), pollerThreadNotifier(), defaultKafkaConsumerConfigurationProperties.getPollerThreadProperties());
    }

    @Bean
    public PollerThreadNotifier pollerThreadNotifier(){
        return new DefaultPollerThreadNotifier();
    }
}
