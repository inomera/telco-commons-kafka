package com.inomera.telco.commons.example.springkafka;


import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessage;
import com.inomera.telco.commons.example.domain.constant.KafkaTopicUtils;
import com.inomera.telco.commons.kafkaprotobuf.ProtobufUtils;
import com.inomera.telco.commons.springkafka.producer.KafkaMessagePublisher;
import lombok.RequiredArgsConstructor;
import messaging.PartitionMessage;


@RequiredArgsConstructor
public class EventPublisher {
    private final KafkaMessagePublisher<? super GeneratedMessage> kafkaPublisher;

    public void fire(GeneratedMessage event) {
        final var topicName = KafkaTopicUtils.getTopicName(event.getClass());
        kafkaPublisher.send(topicName, attachMdcToEvent(event));
    }

    private GeneratedMessage attachMdcToEvent(GeneratedMessage event) {
        final var field = event.getDescriptorForType().findFieldByName("logTrackKey");
        if (field == null) {
            PartitionMessage partition = ProtobufUtils.getField(event, "partition", PartitionMessage.class);
            if (partition == null) {
                return event;
            }
            Descriptors.FieldDescriptor logTrackKey = partition.getDescriptorForType().findFieldByName("log_track_key");
            checkAndSetLogTrackKey(partition, logTrackKey);
            return event;
        }
        return checkAndSetLogTrackKey(event, field);
    }

    private GeneratedMessage checkAndSetLogTrackKey(GeneratedMessage event, Descriptors.FieldDescriptor field) {
        if (field.getJavaType() != Descriptors.FieldDescriptor.JavaType.STRING) {
            return event;
        }
        return (GeneratedMessage) event.toBuilder()
                .setField(field, TransactionKeyUtils.generateTxKey())
                .build();
    }
}
