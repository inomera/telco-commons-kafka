package com.inomera.telco.commons.kafkaprotobuf;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Parser;

public interface ClassIdRegistry {
    Integer getId(Class<? extends GeneratedMessage> classInstance);

    Class<? extends GeneratedMessage> getClass(Integer classId);

    Parser<? extends GeneratedMessage> getParser(Integer classId);
}
