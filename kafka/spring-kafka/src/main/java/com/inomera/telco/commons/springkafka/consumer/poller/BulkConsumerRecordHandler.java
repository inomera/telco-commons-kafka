package com.inomera.telco.commons.springkafka.consumer.poller;

import com.inomera.telco.commons.springkafka.consumer.invoker.BulkInvokerResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Set;
import java.util.concurrent.Future;


public interface BulkConsumerRecordHandler {
    Future<BulkInvokerResult> handle(Set<ConsumerRecord<String, ?>> records);
}
