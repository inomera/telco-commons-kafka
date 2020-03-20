package com.inomera.telco.commons.example.springkafka;

import com.inomera.telco.commons.example.springkafka.msg.Message;
import com.inomera.telco.commons.lang.thread.ThreadUtils;
import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

/**
 * @author Serdar Kuzucu
 */
@Component
@RequiredArgsConstructor
public class SomeListener {
    private static final Logger LOG = LoggerFactory.getLogger(SomeListener.class);

    private final CountDownLatch countDownLatch;

    @KafkaListener(groupId = "event-logger", topics = {"mouse-event.click", "mouse-event.dblclick"}, includeSubclasses = true)
    public void handle(Message message) {
        LOG.info("handle : message={}", message.getClass().getSimpleName());
        countDownLatch.countDown();
        ThreadUtils.sleepQuietly(10);
    }
}
