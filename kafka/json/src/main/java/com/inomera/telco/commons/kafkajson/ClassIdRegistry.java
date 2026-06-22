package com.inomera.telco.commons.kafkajson;

public interface ClassIdRegistry {
    Integer getId(Class<?> classInstance);

    Class<?> getClass(Integer classId);
}
