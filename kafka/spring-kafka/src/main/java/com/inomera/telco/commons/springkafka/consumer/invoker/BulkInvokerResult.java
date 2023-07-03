package com.inomera.telco.commons.springkafka.consumer.invoker;

import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Set;

@Getter
@Setter
@RequiredArgsConstructor
public class BulkInvokerResult {
    private final Set<ConsumerRecord<String, ?>> records;
    private KafkaListener kafkaListener;
}
