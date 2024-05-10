package com.inomera.telco.commons.example.springkafka;

import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessageV3;
import com.inomera.echo.domain.KafkaTopicUtils;
import com.inomera.telco.commons.springkafka.producer.KafkaMessagePublisher;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class EventPublisher {
    private final KafkaMessagePublisher<? super GeneratedMessageV3> kafkaPublisher;

    public void fire(GeneratedMessageV3 event) {
        final var topicName = KafkaTopicUtils.getTopicName(event.getClass());
        kafkaPublisher.send(topicName, attachMdcToEvent(event));
    }

    private GeneratedMessageV3 attachMdcToEvent(GeneratedMessageV3 event) {
        final var field = event.getDescriptorForType().findFieldByName("logTrackKey");
        if (field == null) {
            return event;
        }
        if (field.getJavaType() != Descriptors.FieldDescriptor.JavaType.STRING) {
            return event;
        }
        return (GeneratedMessageV3) event.toBuilder()
                .setField(field, TransactionKeyUtils.generateTxKey())
                .build();
    }
}
