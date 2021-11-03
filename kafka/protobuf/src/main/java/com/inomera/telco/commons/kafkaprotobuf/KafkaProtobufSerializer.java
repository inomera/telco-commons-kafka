package com.inomera.telco.commons.kafkaprotobuf;

import com.google.protobuf.GeneratedMessageV3;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class KafkaProtobufSerializer implements Serializer<GeneratedMessageV3> {
    private final ClassIdRegistry classIdRegistry;

    public KafkaProtobufSerializer(ClassIdRegistry classIdRegistry) {
        this.classIdRegistry = classIdRegistry;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, GeneratedMessageV3 data) {
        final Integer id = classIdRegistry.getId(data.getClass());
        if (id == null) {
            throw new IllegalArgumentException("Class " + data.getClass().getName()
                    + " is not registered to classIdRegistry!");
        }

        try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
             final DataOutputStream dout = new DataOutputStream(out)) {
            dout.writeInt(id);
            dout.write(data.toByteArray());
            dout.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot serialize class " + data.getClass().getName()
                    + ", error=" + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
    }
}
