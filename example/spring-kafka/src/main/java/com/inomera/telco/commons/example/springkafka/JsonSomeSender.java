package com.inomera.telco.commons.example.springkafka;

import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedBeautifullyMessage;
import com.inomera.telco.commons.springkafka.producer.KafkaMessagePublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;


@Component
@RequiredArgsConstructor
public class JsonSomeSender {
    private static final Logger LOG = LoggerFactory.getLogger(JsonSomeSender.class);

    @Qualifier("jsonKafkaMessagePublisher")
    private final KafkaMessagePublisher jsonKafkaMessagePublisher;
    public final AtomicInteger atomicInteger = new AtomicInteger(1);

    @Scheduled(fixedDelay = 1000)
    public void publishRandomText() {
	LOG.info("Sending event");
	for (int i = 0; i < 4; i++) {
	    final int value = atomicInteger.incrementAndGet();
	    jsonKafkaMessagePublisher.send("user-events", new SomethingHappenedBeautifullyMessage(value + "-" + TransactionKeyUtils.generateTxKey()));
	}
	LOG.info("Sent event");
    }

}
