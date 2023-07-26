package com.inomera.telco.commons.springkafka.consumer.retry;

import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import com.inomera.telco.commons.springkafka.consumer.invoker.BulkInvokerResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.RetriableCommitFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.inomera.telco.commons.springkafka.consumer.retry.DefaultInMemoryBulkRecordRetryConsumer.bulkRetryQueue;


public class DefaultBulkRecordRetryer implements BulkRecordRetryer {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultBulkRecordRetryer.class);

    @Override
    public void checkAndRetry(BulkInvokerResult result) {
	final KafkaListener kafkaListener = result.getKafkaListener();
	if (kafkaListener == null) {
	    return;
	}

	if (kafkaListener != null && kafkaListener.retry() == KafkaListener.RETRY.NONE) {
	    return;
	}

	final Set<ConsumerRecord<String, ?>> records = result.getRecords();
	final ConsumerRecord<String, ?> record = records.iterator().next();
	final String topic = record.topic();
	if (kafkaListener != null && kafkaListener.retry() == KafkaListener.RETRY.RETRY_FROM_BROKER) {
	    LOG.warn("before ack/commit to broker, message : {} retrying for the topic : {}, if the consumer re-start or re-subscribe another consumer in consumer group, try to process", records, topic);
	    throw RetriableCommitFailedException.withUnderlyingMessage("Retry message offset " + record.offset() + " for topic " + topic);
	}
	if (kafkaListener != null && kafkaListener.retry() == KafkaListener.RETRY.RETRY_IN_MEMORY_TASK) {
	    final BulkRetryContext retryContext = new BulkRetryContext();
	    retryContext.setCount(0);
	    retryContext.setBackoffTime(kafkaListener.retryBackoffTime());
	    retryContext.setMaxCount(kafkaListener.retryCount());
	    retryContext.setRetry(result);
	    bulkRetryQueue.offer(retryContext);
	}
    }
}
