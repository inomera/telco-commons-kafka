package com.inomera.telco.commons.example.springkafka;

import com.inomera.telco.commons.example.springkafka.msg.Message;
import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedConsumerMessage;
import com.inomera.telco.commons.lang.thread.ThreadUtils;
import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.inomera.telco.commons.springkafka.annotation.KafkaListener.RETRY.NONE;
import static com.inomera.telco.commons.springkafka.annotation.KafkaListener.RETRY.RETRY_FROM_BROKER;

/**
 * @author Serdar Kuzucu
 */
@Component
public class SomeListener {
    private static final Logger LOG = LoggerFactory.getLogger(SomeListener.class);

    @KafkaListener(groupId = "event-logger", topics = {"mouse-event.click", "mouse-event.dblclick"}, includeSubclasses = true, retry = RETRY_FROM_BROKER)
    public void handle(Message message) {
	LOG.info("handle : message={}", message);
	ThreadUtils.sleepQuietly(300);
	if (message instanceof SomethingHappenedConsumerMessage) {
	    throw new RuntimeException("retry test");
	}
    }

    @KafkaListener(groupId = "bulk-event-logger", topics = {"mouse-bulk-event.click", "mouse-bulk-event.dblclick"}, includeSubclasses = true, retry = NONE)
    public void bulkHandle(List<Message> messages) {
	final Message message = messages.get(0);
	LOG.info("handle : message={}, messageCount={}", message, messages.size());
	ThreadUtils.sleepQuietly(300);
	if (message instanceof SomethingHappenedConsumerMessage) {
	    throw new RuntimeException("retry test");
	}
    }
}
