package com.inomera.telco.commons.kafkakryo;

import com.esotericsoftware.kryo.io.ByteBufferOutput;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serializer;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Ramazan Karakaya
 * @author Mustafa Genc
 * @author Serdar Kuzucu
 */
@RequiredArgsConstructor
public class KryoKafkaSerializer implements Serializer<Serializable> {
    private final KryoFactory kryoFactory;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, Serializable data) {
        final ByteBufferOutput output = new ByteBufferOutput(24, 1024000);
        kryoFactory.getKryo().writeClassAndObject(output, data);
        output.flush();
        return output.toBytes();
    }

    @Override
    public void close() {

    }

}
