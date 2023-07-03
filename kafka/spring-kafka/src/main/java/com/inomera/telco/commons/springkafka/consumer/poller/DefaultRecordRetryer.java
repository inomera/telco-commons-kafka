package com.inomera.telco.commons.springkafka.consumer.poller;

import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.RetriableCommitFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultRecordRetryer implements RecordRetryer {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultRecordRetryer.class);

    private final Map<Long, AtomicInteger> retryMap = new ConcurrentHashMap<>(10);
    private final ConsumerRecordHandler consumerRecordHandler;

    public DefaultRecordRetryer(ConsumerRecordHandler consumerRecordHandler) {
	this.consumerRecordHandler = consumerRecordHandler;
    }

    @Override
    public void checkAndRetry(InvokerResult result) {
	final KafkaListener kafkaListener = result.getKafkaListener();
	if (kafkaListener == null) {
	    return;
	}

	if (kafkaListener != null && kafkaListener.retry() == KafkaListener.RETRY.NONE) {
	    return;
	}

	final ConsumerRecord<String, ?> record = result.getRecord();
	if (kafkaListener != null && kafkaListener.retry() == KafkaListener.RETRY.RETRY_FROM_BROKER) {
	    final AtomicInteger actualCount = retryMap.putIfAbsent(Long.valueOf(record.offset()), new AtomicInteger(0));
	    if (actualCount.incrementAndGet() >= kafkaListener.retryCount()) {
		LOG.warn(" the message : {} is reached the retry count limit for the topic : {}", record, record.topic());
		return;
	    }
	    LOG.warn("before ack/commit to broker, message : {} retrying for the topic : {}, if the consumer re-start or re-subscribe another consumer in consumer group, try to process", record, record.topic());
	    throw RetriableCommitFailedException.withUnderlyingMessage("Retry message offset " + record.offset() + " for topic " + record.topic());
	}
	if (kafkaListener != null && kafkaListener.retry() == KafkaListener.RETRY.RETRY_IN_MEMORY_TASK) {
	    final AtomicInteger actualCount = retryMap.putIfAbsent(Long.valueOf(record.offset()), new AtomicInteger(0));
	    if (actualCount.incrementAndGet() >= kafkaListener.retryCount()) {
		LOG.warn(" the first one of the messages : {} is reached the retry count limit for the topic : {}", record, record.topic());
		return;
	    }
	    LOG.warn("message remove without commit for processing the topic : {}, if the re-submission message with retry task in consumer group, try to process", record.topic());
	    consumerRecordHandler.handle(record);
	}
    }
}
