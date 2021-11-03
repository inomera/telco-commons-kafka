package com.inomera.telco.commons.kafkaprotobuf;

import com.google.protobuf.GeneratedMessageV3;
import com.inomera.telco.commons.kafkaprotobuf.protomodel.AuthenticationInfoProto;
import com.inomera.telco.commons.kafkaprotobuf.protomodel.UserSessionInfoProto;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class KafkaProtobufSerializerTest {
    @Test
    void shouldSerialize() {
        final Map<Class<? extends GeneratedMessageV3>, Integer> classMap = new LinkedHashMap<>();
        classMap.put(UserSessionInfoProto.class, 17);
        final ImmutableClassIdRegistry classIdRegistry = new ImmutableClassIdRegistry(classMap);
        final KafkaProtobufSerializer kafkaProtobufSerializer = new KafkaProtobufSerializer(classIdRegistry);

        final UserSessionInfoProto userSessionInfo = UserSessionInfoProto.newBuilder()
                .setAuthenticationInfo(AuthenticationInfoProto.newBuilder()
                        .setAdslNo("adslNo")
                        .setFirstName("Atakan")
                        .setLastName("Ulker"))
                .setDeviceId("device-id")
                .build();

        final byte[] userSessionInfoSerializedBytes = userSessionInfo.toByteArray();
        final byte[] classIdSerializedBytes = ByteBuffer.allocate(4).putInt(17).array();
        final byte[] expectedResult = ArrayUtils.addAll(classIdSerializedBytes, userSessionInfoSerializedBytes);

        final byte[] actualResult = kafkaProtobufSerializer.serialize("any-topic", userSessionInfo);
        assertArrayEquals(expectedResult, actualResult);
    }
}
