package com.inomera.telco.commons.kafkajson;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Serdar Kuzucu
 */
class JsonKafkaDeserializerTest {
    @Test
    void shouldDeserializeSmileFormatCorrectly() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();

        final ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(81);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(byteBuffer.array());
        objectMapper.writeValue(outputStream, new SimplePojo(3L, "Turgay", "Can"));
        final byte[] bytes = outputStream.toByteArray();

        final Map<Class<?>, Integer> classIdMap = new HashMap<>();
        classIdMap.put(SimplePojo.class, 81);
        final JsonKafkaDeserializer smileKafkaDeserializer = new JsonKafkaDeserializer(new ImmutableClassIdRegistry(classIdMap));

        final SimplePojo deserialized = (SimplePojo) smileKafkaDeserializer.deserialize("anyTopic", bytes);
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
