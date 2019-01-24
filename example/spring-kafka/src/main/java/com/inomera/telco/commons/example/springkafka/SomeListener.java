package com.inomera.telco.commons.example.springkafka;

import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Serdar Kuzucu
 */
@Component
public class SomeListener {
    private static final Logger LOG = LoggerFactory.getLogger(SomeListener.class);

    @KafkaListener(groupId = "event-saver", topics = "mouse-event.click")
    public void handle1(String message) {
        LOG.info("handle1 : event-saver : mouse-event.click : message={}", message);
    }

    @KafkaListener(groupId = "event-saver", topics = "mouse-event.click")
    public void handle2(String message) {
        LOG.info("handle2 : event-saver : mouse-event.click : message={}", message);
    }

    @KafkaListener(groupId = "event-handler", topics = "mouse-event.click")
    public void handle3(String message) {
        LOG.info("handle3 : event-handler : mouse-event.click : message={}", message);
    }
}
