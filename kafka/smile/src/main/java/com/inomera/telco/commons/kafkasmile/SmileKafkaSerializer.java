package com.inomera.telco.commons.kafkasmile;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import static com.inomera.telco.commons.kafkasmile.SmileSerializationConstants.CLASS_ID_SIZE_IN_BYTES;

/**
 * @author Serdar Kuzucu
 */
@RequiredArgsConstructor
public class SmileKafkaSerializer implements Serializer<Object> {
    private final ClassIdRegistry classIdRegistry;
    private final ObjectMapper objectMapper = SmileObjectMapperFactory.create();

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

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(ByteBuffer.allocate(CLASS_ID_SIZE_IN_BYTES).putInt(id).array());
            objectMapper.writeValue(out, data);
            final byte[] bytes = out.toByteArray();
            out.close();
            return bytes;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void close() {
    }
}
