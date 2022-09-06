package com.inomera.telco.commons.example.springkafka;

import com.inomera.telco.commons.example.springkafka.msg.Message;
import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedConsumerMessage;
import com.inomera.telco.commons.lang.thread.ThreadUtils;
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

    @KafkaListener(groupId = "event-logger", topics = {"mouse-event.click", "mouse-event.dblclick"}, includeSubclasses = true, retry = true)
    public void handle(Message message) {
        LOG.info("handle : message={}", message.getClass().getSimpleName());
        ThreadUtils.sleepQuietly(300);
        if (message instanceof SomethingHappenedConsumerMessage) {
            throw new RuntimeException("retry test");
        }
    }
}
