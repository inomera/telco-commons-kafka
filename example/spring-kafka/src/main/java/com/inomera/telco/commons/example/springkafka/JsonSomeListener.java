package com.inomera.telco.commons.example.springkafka;

import com.inomera.telco.commons.example.springkafka.msg.Message;
import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.inomera.telco.commons.springkafka.annotation.KafkaListener.RETRY.RETRY_IN_MEMORY_TASK;


@Component
public class JsonSomeListener {
    private static final Logger LOG = LoggerFactory.getLogger(JsonSomeListener.class);

    @KafkaListener(groupId = "json-logger", topics = {"user-events"}, includeSubclasses = true, retry = RETRY_IN_MEMORY_TASK, retryCount = 5, retryBackoffTime = 5000L)
    public void handle(Message message) {
	LOG.info("handle : jsonMessage={}", message);
    }


}
