package com.inomera.telco.commons.kafkaprotobuf;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import static com.inomera.telco.commons.kafkaprotobuf.ProtobufSerializationConstants.CLASS_ID_SIZE_IN_BYTES;

public class KafkaProtobufDeserializer implements Deserializer<GeneratedMessageV3> {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaProtobufDeserializer.class);

    private final ClassIdRegistry classIdRegistry;

    public KafkaProtobufDeserializer(ClassIdRegistry classIdRegistry) {
        this.classIdRegistry = classIdRegistry;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public GeneratedMessageV3 deserialize(String topic, byte[] data) {
        try (final DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            final int classId = getClassId(data, in);
            final Parser<? extends GeneratedMessageV3> parser = getParser(classId);
            return parseWithParser(parser, classId, data);
        } catch (Exception e) {
            // Throwing exception in deserialize causes consumer to terminate itself.
            // Do not throw exception from here. Just log it.
            LOG.error("Error in deserialize event of topic {}: {}", topic, e.getMessage(), e);
            return null;
        }
    }

    private GeneratedMessageV3 parseWithParser(Parser<? extends GeneratedMessageV3> parser,
                                               int classId, byte[] data) {
        try {
            return parser.parseFrom(data, CLASS_ID_SIZE_IN_BYTES, data.length - CLASS_ID_SIZE_IN_BYTES);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("InvalidProtocolBufferException: Can not read proto message with id="
                    + classId + ". data=" + Base64.getEncoder().encodeToString(data), e);
        }
    }

    private int getClassId(byte[] data, DataInputStream in) {
        final int classId;
        try {
            classId = in.readInt();
        } catch (IOException e) {
            throw new IllegalArgumentException("Can not read proto message id. data="
                    + Base64.getEncoder().encodeToString(data), e);
        }
        return classId;
    }

    private Parser<? extends GeneratedMessageV3> getParser(int classId) {
        final Parser<? extends GeneratedMessageV3> parser = classIdRegistry.getParser(classId);
        if (parser == null) {
            throw new IllegalArgumentException("Can not find proto parser in classIdRegistry. classId=" + classId);
        }
        return parser;
    }

    @Override
    public void close() {
    }
}
