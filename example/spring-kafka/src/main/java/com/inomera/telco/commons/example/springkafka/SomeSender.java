package com.inomera.telco.commons.example.springkafka;

import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedBeautifullyMessage;
import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedMessage;
import com.inomera.telco.commons.example.springkafka.msg.UnListenedMessage;
import com.inomera.telco.commons.springkafka.producer.KafkaMessagePublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Serdar Kuzucu
 */
@Component
@RequiredArgsConstructor
public class SomeSender {
    private static final Logger LOG = LoggerFactory.getLogger(SomeSender.class);

    private final KafkaMessagePublisher<Serializable> kafkaMessagePublisher;
    private final AtomicInteger atomicInteger = new AtomicInteger(1);

    @Scheduled(fixedDelay = 1000)
    public void publishRandomText() {
        LOG.debug("Sending event");
        for (int i = 0; i < 20; i++) {
            final int value = atomicInteger.incrementAndGet();
            if (value % 3 == 0) {
                kafkaMessagePublisher.send("mouse-event.click", new SomethingHappenedMessage());
            } else if (value % 3 == 1) {
                kafkaMessagePublisher.send("mouse-event.dblclick", new SomethingHappenedBeautifullyMessage());
            } else {
                kafkaMessagePublisher.send("example.unlistened-topic", new UnListenedMessage());
            }
        }
        LOG.debug("Sent event");
    }
}
