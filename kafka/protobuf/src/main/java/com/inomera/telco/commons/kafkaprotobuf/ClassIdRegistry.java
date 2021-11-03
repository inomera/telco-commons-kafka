package com.inomera.telco.commons.kafkaprotobuf;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Parser;

public interface ClassIdRegistry {
    Integer getId(Class<? extends GeneratedMessageV3> classInstance);

    Class<? extends GeneratedMessageV3> getClass(Integer classId);

    Parser<? extends GeneratedMessageV3> getParser(Integer classId);
}
