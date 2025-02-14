package com.inomera.telco.commons.kafkaprotobuf;

import com.google.protobuf.GeneratedMessage;
import com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto;
import com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProto;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaProtobufDeserializerTest {

    @Test
    void shouldDeserialize() {
        final Map<Class<? extends GeneratedMessage>, Integer> classMap = new LinkedHashMap<>();
        classMap.put(UserSessionInfoProto.class, 17);
        final ImmutableClassIdRegistry classIdRegistry = new ImmutableClassIdRegistry(classMap);
        final UserSessionInfoProto deserializedUserSessionInfo;
        try (KafkaProtobufDeserializer kafkaProtobufDeserializer = new KafkaProtobufDeserializer(classIdRegistry)) {

            final UserSessionInfoProto userSessionInfo = UserSessionInfoProto.newBuilder()
                    .setAuthenticationInfo(AuthenticationInfoProto.newBuilder()
                            .setAdslNo("adslNo")
                            .setFirstName("Atakan")
                            .setLastName("Ulker"))
                    .setDeviceId("device-id")
                    .build();
            final byte[] userSessionInfoSerializedBytes = userSessionInfo.toByteArray();
            final byte[] classIdSerializedBytes = ByteBuffer.allocate(4).putInt(17).array();
            final byte[] serializedData = ArrayUtils.addAll(classIdSerializedBytes, userSessionInfoSerializedBytes);
            deserializedUserSessionInfo = (UserSessionInfoProto) kafkaProtobufDeserializer
                    .deserialize("any-topic", serializedData);
        }
        assertEquals("device-id", deserializedUserSessionInfo.getDeviceId());
        assertEquals("adslNo", deserializedUserSessionInfo.getAuthenticationInfo().getAdslNo());
        assertEquals("Atakan", deserializedUserSessionInfo.getAuthenticationInfo().getFirstName());
        assertEquals("Ulker", deserializedUserSessionInfo.getAuthenticationInfo().getLastName());
    }
}
