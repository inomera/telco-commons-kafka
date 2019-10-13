package com.inomera.telco.commons.example.springkafka;

import com.inomera.telco.commons.example.springkafka.msg.Message;
import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedBeautifullyMessage;
import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedMessage;
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
    public void handle1(SomethingHappenedMessage message) {
        LOG.info("handle1 : event-saver : mouse-event.click : message={}", message);
    }

    @KafkaListener(groupId = "event-saver", topics = "mouse-event.click")
    public void handle2(SomethingHappenedMessage message) {
        LOG.info("handle2 : event-saver : mouse-event.click : message={}", message);
    }

    @KafkaListener(groupId = "event-handler", topics = "mouse-event.click")
    public void handle3(SomethingHappenedMessage message) {
        LOG.info("handle3 : event-handler : mouse-event.click : message={}", message);
    }

    @KafkaListener(groupId = "event-saver", topics = "mouse-event.click")
    public void handle4(SomethingHappenedBeautifullyMessage message) {
        LOG.info("handle4 : event-saver : mouse-event.click : message={}", message);
    }

    @KafkaListener(groupId = "event-saver", topics = "mouse-event.click")
    public void handle5(SomethingHappenedBeautifullyMessage message) {
        LOG.info("handle5 : event-saver : mouse-event.click : message={}", message);
    }

    @KafkaListener(groupId = "event-handler", topics = "mouse-event.click")
    public void handle6(SomethingHappenedBeautifullyMessage message) {
        LOG.info("handle6 : event-handler : mouse-event.click : message={}", message);
    }

    @KafkaListener(groupId = "event-saver", topics = "mouse-event.click", includeSubclasses = true)
    public void handle7(Message message) {
        LOG.info("handle7 : event-saver : mouse-event.click : message={}", message);
    }
}
