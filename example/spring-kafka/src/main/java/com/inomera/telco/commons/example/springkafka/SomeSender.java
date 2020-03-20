package com.inomera.telco.commons.example.springkafka;

import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedBeautifullyMessage;
import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedMessage;
import com.inomera.telco.commons.springkafka.producer.KafkaMessagePublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Serdar Kuzucu
 */
@Component
@RequiredArgsConstructor
public class SomeSender {
    private static final Logger LOG = LoggerFactory.getLogger(SomeSender.class);

    private final KafkaMessagePublisher<Serializable> kafkaMessagePublisher;
    private final CountDownLatch countDownLatch;

    @PostConstruct
    public void publishRandomText() {
        new Thread(() -> {
            LOG.debug("Sending event");
            for (int i = 0; i < 10_000; i++) {
                kafkaMessagePublisher.send("mouse-event.click", new SomethingHappenedMessage());
                kafkaMessagePublisher.send("mouse-event.dblclick", new SomethingHappenedBeautifullyMessage());
            }
            try {
                countDownLatch.await();
                LOG.info("Finito");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            LOG.debug("Sent event");
        }).start();
    }
}
