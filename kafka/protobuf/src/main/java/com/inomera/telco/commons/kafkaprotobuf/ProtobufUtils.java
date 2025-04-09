package com.inomera.telco.commons.kafkaprotobuf;

import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessage;

public final class ProtobufUtils {

    public static <T> T getField(GeneratedMessage message, String fieldName, Class<T> clazz) {
        try {
            Descriptors.FieldDescriptor fieldDescriptor = message.getDescriptorForType().findFieldByName(fieldName);
            if (fieldDescriptor != null && message.hasField(fieldDescriptor)) {
                return clazz.cast(message.getField(fieldDescriptor));
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
