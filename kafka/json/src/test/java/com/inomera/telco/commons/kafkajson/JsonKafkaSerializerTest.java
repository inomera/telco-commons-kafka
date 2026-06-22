package com.inomera.telco.commons.kafkajson;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


class JsonKafkaSerializerTest {
    @Test
    @DisplayName("Should be able to serialize a simple pojo in binary json format")
    void shouldSerializeCorrectlyInSmileFormat() throws IOException {
        final Map<Class<?>, Integer> classIdMap = new HashMap<>();
        classIdMap.put(SimplePojo.class, 81);
        final JsonKafkaSerializer smileKafkaSerializer = new JsonKafkaSerializer(new ImmutableClassIdRegistry(classIdMap));

        final SimplePojo simplePojo = new SimplePojo(3L, "Turgay", "Can");
        final byte[] serializedData = smileKafkaSerializer.serialize("anyTopic", simplePojo);

        final ByteBuffer byteBuffer = ByteBuffer.wrap(serializedData);
        assertEquals(81, byteBuffer.getInt());

        final ObjectMapper objectMapper = new ObjectMapper();
        final SimplePojo deserialized = objectMapper.readValue(
                serializedData, byteBuffer.position(), byteBuffer.remaining(), SimplePojo.class);
        assertEquals(3L, deserialized.getId());
        assertEquals("Turgay", deserialized.getFirstName());
        assertEquals("Can", deserialized.getLastName());
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimplePojo {
        private long id;
        private String firstName;
        private String lastName;
    }
}
