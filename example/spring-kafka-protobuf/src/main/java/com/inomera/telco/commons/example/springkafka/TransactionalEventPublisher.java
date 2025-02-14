package com.inomera.telco.commons.example.springkafka;


import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessage;
import com.inomera.telco.commons.example.domain.constant.KafkaTopicUtils;
import com.inomera.telco.commons.springkafka.producer.KafkaTransactionalMessagePublisher;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class TransactionalEventPublisher {
    private final KafkaTransactionalMessagePublisher<? super GeneratedMessage> kafkaPublisher;

    public void fire(GeneratedMessage event) {
        final var topicName = KafkaTopicUtils.getTopicName(event.getClass());
        kafkaPublisher.send(topicName, attachMdcToEvent(event));
    }

    private GeneratedMessage attachMdcToEvent(GeneratedMessage event) {
        final var field = event.getDescriptorForType().findFieldByName("logTrackKey");
        if (field == null) {
            return event;
        }
        if (field.getJavaType() != Descriptors.FieldDescriptor.JavaType.STRING) {
            return event;
        }
        return (GeneratedMessage) event.toBuilder()
                .setField(field, TransactionKeyUtils.generateTxKey())
                .build();
    }
}
