package com.inomera.telco.commons.kafkasmile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
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

/**
 * @author Serdar Kuzucu
 */
class SmileKafkaSerializerTest {
    @Test
    @DisplayName("Should be able to serialize a simple pojo in binary json smile format")
    void shouldSerializeCorrectlyInSmileFormat() throws IOException {
        final Map<Class<?>, Integer> classIdMap = new HashMap<>();
        classIdMap.put(SimplePojo.class, 81);
        final SmileKafkaSerializer smileKafkaSerializer = new SmileKafkaSerializer(new ImmutableClassIdRegistry(classIdMap));

        final SimplePojo simplePojo = new SimplePojo(3L, "Serdar", "Kuzucu");
        final byte[] serializedData = smileKafkaSerializer.serialize("anyTopic", simplePojo);

        final ByteBuffer byteBuffer = ByteBuffer.wrap(serializedData);
        assertEquals(81, byteBuffer.getInt());

        final ObjectMapper objectMapper = new ObjectMapper(new SmileFactory());
        final SimplePojo deserialized = objectMapper.readValue(
                serializedData, byteBuffer.position(), byteBuffer.remaining(), SimplePojo.class);
        assertEquals(3L, deserialized.getId());
        assertEquals("Serdar", deserialized.getFirstName());
        assertEquals("Kuzucu", deserialized.getLastName());
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
