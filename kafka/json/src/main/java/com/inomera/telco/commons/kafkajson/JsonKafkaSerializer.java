package com.inomera.telco.commons.kafkajson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.inomera.telco.commons.kafkajson.JsonSerializationConstants.CLASS_ID_SIZE_IN_BYTES;

public class JsonKafkaSerializer implements Serializer<Object> {
    private ObjectMapper objectMapper;
    private ClassIdRegistry classIdRegistry;

    public JsonKafkaSerializer() {
	this.objectMapper = JsonObjectMapperFactory.create();
	this.classIdRegistry = new ImmutableClassIdRegistry(new HashMap<>());
    }

    public JsonKafkaSerializer(ClassIdRegistry classIdRegistry) {
	this();
	this.classIdRegistry = classIdRegistry;
    }

    public JsonKafkaSerializer(ClassIdRegistry classIdRegistry, ObjectMapper objectMapper) {
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
    public byte[] serialize(String topic, Object data) {
	try {
	    final Integer id = classIdRegistry.getId(data.getClass());
	    if (id == null) {
		throw new IllegalArgumentException("Class " + data.getClass() + " is not registered!");
	    }

	    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
		out.write(ByteBuffer.allocate(CLASS_ID_SIZE_IN_BYTES).putInt(id).array());
		objectMapper.writeValue(out, data);
		final byte[] bytes = out.toByteArray();
		return bytes;
	    }
	} catch (IOException e) {
	    throw new IllegalArgumentException(e);
	}
    }

    @Override
    public void close() {
    }
}
