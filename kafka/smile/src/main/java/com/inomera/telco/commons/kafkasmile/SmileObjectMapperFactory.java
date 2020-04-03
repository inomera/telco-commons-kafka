package com.inomera.telco.commons.kafkasmile;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

/**
 * @author Serdar Kuzucu
 */
public class SmileObjectMapperFactory {
    public static ObjectMapper create() {
        final ObjectMapper objectMapper = new ObjectMapper(new SmileFactory());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }
}
