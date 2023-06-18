package com.inomera.telco.commons.springkafka.consumer.poller;

import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.List;
import java.util.concurrent.Future;


public interface ConsumerBulkRecordHandler {
    Future<InvokerResult> handle(List<ConsumerRecord<String, ?>> record);
}
