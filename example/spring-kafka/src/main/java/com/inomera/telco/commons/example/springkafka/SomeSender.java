package com.inomera.telco.commons.example.springkafka;

import com.inomera.telco.commons.springkafka.producer.KafkaMessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Serdar Kuzucu
 */
@Component
public class SomeSender {
    private static final Logger LOG = LoggerFactory.getLogger(SomeSender.class);

    private final KafkaMessagePublisher<String> kafkaMessagePublisher;

    public SomeSender(KafkaMessagePublisher<String> kafkaMessagePublisher) {
        this.kafkaMessagePublisher = kafkaMessagePublisher;
    }

    @Scheduled(fixedRate = 2000L)
    public void publishRandomText() {
        LOG.info("Sending event");
        kafkaMessagePublisher.send("mouse-event.click", "x=1;y=2;");
        LOG.info("Sent event");
    }
}
