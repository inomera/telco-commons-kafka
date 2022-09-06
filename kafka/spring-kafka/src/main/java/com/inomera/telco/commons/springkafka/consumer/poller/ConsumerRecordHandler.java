package com.inomera.telco.commons.springkafka.consumer.poller;

import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.concurrent.Future;

/**
 * @author Serdar Kuzucu
 */
public interface ConsumerRecordHandler {
    Future<InvokerResult> handle(ConsumerRecord<String, ?> record);
}
