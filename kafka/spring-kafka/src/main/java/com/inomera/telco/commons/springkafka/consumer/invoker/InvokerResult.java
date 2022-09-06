package com.inomera.telco.commons.springkafka.consumer.invoker;

import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Getter
@Setter
@RequiredArgsConstructor
public class InvokerResult {
    private final ConsumerRecord<String, ?> record;
    private KafkaListener kafkaListener;
}
