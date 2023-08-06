package com.inomera.telco.commons.example.springkafka;

import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedBeautifullyMessage;
import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedConsumerMessage;
import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedMessage;
import com.inomera.telco.commons.lang.thread.ThreadUtils;
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
public class KryoSomeSender {
    private static final Logger LOG = LoggerFactory.getLogger(KryoSomeSender.class);

    private final KafkaMessagePublisher<Serializable> stringKafkaMessagePublisher;
    public final AtomicInteger atomicInteger = new AtomicInteger(1);
    public final AtomicInteger bulkAtomicInteger = new AtomicInteger(1);

    @Scheduled(fixedDelay = 1000)
    public void publishRandomText() {
	LOG.info("Sending event");
	for (int i = 0; i < 4; i++) {
	    ThreadUtils.sleepQuietly(3000);
	    final int value = atomicInteger.incrementAndGet();
	    if (value % 3 == 0) {
		stringKafkaMessagePublisher.send("mouse-event.click", new SomethingHappenedMessage(value + "-" + TransactionKeyUtils.generateTxKey()));
	    } else if (value % 3 == 1) {
		stringKafkaMessagePublisher.send("mouse-event.dblclick", new SomethingHappenedBeautifullyMessage(value + "-" + TransactionKeyUtils.generateTxKey()));
	    } else if (value % 3 == 2) {
		stringKafkaMessagePublisher.send("mouse-event.dblclick", new SomethingHappenedConsumerMessage(value + "-" + TransactionKeyUtils.generateTxKey()));
	    } else {
		stringKafkaMessagePublisher.send("example.unlistened-topic", new UnListenedMessage(value + "-" + TransactionKeyUtils.generateTxKey()));
	    }
	}
	LOG.info("Sent event");
    }

    @Scheduled(fixedDelay = 1000)
    public void publishRandomBulkText() {
	LOG.info("Sending event");
	for (int i = 0; i < 1; i++) {
	    final int value = bulkAtomicInteger.incrementAndGet();
	    stringKafkaMessagePublisher.send("mouse-bulk-event.click", new SomethingHappenedMessage(value + "-" + TransactionKeyUtils.generateTxKey()));
	    stringKafkaMessagePublisher.send("mouse-bulk-event.dblclick", new SomethingHappenedBeautifullyMessage(value + "-" + TransactionKeyUtils.generateTxKey()));
	    stringKafkaMessagePublisher.send("mouse-bulk-event.dblclick", new SomethingHappenedConsumerMessage(value + "-" + TransactionKeyUtils.generateTxKey()));
	    stringKafkaMessagePublisher.send("bulk-example.unlistened-topic", new UnListenedMessage(value + "-" + TransactionKeyUtils.generateTxKey()));
	}
	LOG.info("Sent event");
    }
}
