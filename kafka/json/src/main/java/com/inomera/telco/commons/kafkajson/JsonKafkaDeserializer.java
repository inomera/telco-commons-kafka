package com.inomera.telco.commons.kafkajson;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

import java.nio.ByteBuffer;
import java.util.Map;


@Slf4j
public class JsonKafkaDeserializer implements Deserializer<Object> {
    private ClassIdRegistry classIdRegistry;
    private ObjectMapper objectMapper;

    public JsonKafkaDeserializer() {
        this.objectMapper = JsonObjectMapperFactory.create();
    }

    public JsonKafkaDeserializer(ClassIdRegistry classIdRegistry) {
        this();
        this.classIdRegistry = classIdRegistry;
    }

    public JsonKafkaDeserializer(ClassIdRegistry classIdRegistry, ObjectMapper objectMapper) {
        this.classIdRegistry = classIdRegistry;
        this.objectMapper = objectMapper;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

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
