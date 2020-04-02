package com.inomera.telco.commons.kafkasmile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
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
class SmileKafkaDeserializerTest {
    @Test
    void shouldDeserializeSmileFormatCorrectly() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper(new SmileFactory());

        final ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(81);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(byteBuffer.array());
        objectMapper.writeValue(outputStream, new SimplePojo(3L, "Serdar", "Kuzucu"));
        final byte[] bytes = outputStream.toByteArray();

        final Map<Class<?>, Integer> classIdMap = new HashMap<>();
        classIdMap.put(SimplePojo.class, 81);
        final SmileKafkaDeserializer smileKafkaDeserializer = new SmileKafkaDeserializer(new ImmutableClassIdRegistry(classIdMap));

        final SimplePojo deserialized = (SimplePojo) smileKafkaDeserializer.deserialize("anyTopic", bytes);
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
