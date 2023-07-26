package com.inomera.telco.commons.springkafka.consumer.invoker;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Set;
import java.util.concurrent.Future;


public interface BulkConsumerInvoker {
    void start();

    void stop();

    Future<BulkInvokerResult> invoke(Set<ConsumerRecord<String, ?>> records);
}
