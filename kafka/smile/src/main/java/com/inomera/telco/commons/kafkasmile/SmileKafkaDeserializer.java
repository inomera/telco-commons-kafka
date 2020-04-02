package com.inomera.telco.commons.kafkasmile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author Serdar Kuzucu
 */
@RequiredArgsConstructor
public class SmileKafkaDeserializer implements Deserializer<Object> {
    private static final Logger LOG = LoggerFactory.getLogger(SmileKafkaDeserializer.class);
    private final ClassIdRegistry classIdRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper(new SmileFactory());

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public Object deserialize(String topic, byte[] data) {
        try {
            final ByteBuffer byteBuffer = ByteBuffer.wrap(data);
            final Integer id = byteBuffer.getInt();
            final Class<?> classToDeserialize = classIdRegistry.getClass(id);
            if (classToDeserialize == null) {
                throw new IllegalArgumentException("Class is not registered for id " + id);
            }
            return objectMapper.readValue(data, byteBuffer.position(), byteBuffer.remaining(), classToDeserialize);
        } catch (Exception e) {
            // Throwing exception in deserialize causes consumer to terminate itself.
            // Do not throw exception from here. Just log it.
            LOG.error("Error in deserialize event of topic {}: {}", topic, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void close() {
    }
}
