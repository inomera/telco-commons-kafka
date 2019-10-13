package com.inomera.telco.commons.example.springkafka;

import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedBeautifullyMessage;
import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedMessage;
import com.inomera.telco.commons.springkafka.producer.KafkaMessagePublisher;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * @author Serdar Kuzucu
 */
@Component
public class SomeSender {
    private static final Logger LOG = LoggerFactory.getLogger(SomeSender.class);

    private final KafkaMessagePublisher<Serializable> kafkaMessagePublisher;

    public SomeSender(KafkaMessagePublisher<Serializable> kafkaMessagePublisher) {
        this.kafkaMessagePublisher = kafkaMessagePublisher;
    }

    @Scheduled(fixedRate = 2000L)
    public void publishRandomText() {
        LOG.info("Sending event");
        if (RandomUtils.nextBoolean()) {
            kafkaMessagePublisher.send("mouse-event.click", new SomethingHappenedMessage());
        } else {
            kafkaMessagePublisher.send("mouse-event.click", new SomethingHappenedBeautifullyMessage());
        }
        LOG.info("Sent event");
    }
}
