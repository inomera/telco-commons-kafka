package com.inomera.telco.commons.kafkasmile;

/**
 * @author Serdar Kuzucu
 */
public interface ClassIdRegistry {
    Integer getId(Class<?> classInstance);

    Class<?> getClass(Integer classId);
}
