package com.inomera.telco.commons.kafkakryo;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Ramazan Karakaya
 * @author Serdar Kuzucu
 */
@RequiredArgsConstructor
public class KryoKafkaDeserializer implements Deserializer<Serializable> {
    private static final Logger LOG = LoggerFactory.getLogger(KryoKafkaDeserializer.class);
    private final KryoFactory kryoFactory;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public Serializable deserialize(String topic, byte[] data) {
        try {
            final ByteBufferInput input = new ByteBufferInput(data);
            return (Serializable) kryoFactory.getKryo().readClassAndObject(input);
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
