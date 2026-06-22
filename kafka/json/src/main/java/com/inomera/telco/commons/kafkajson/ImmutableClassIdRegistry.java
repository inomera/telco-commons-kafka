package com.inomera.telco.commons.kafkajson;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


public class ImmutableClassIdRegistry implements ClassIdRegistry {
    private final Map<Class<?>, Integer> classIdMapping;
    private final Map<Integer, Class<?>> idClassMapping;


    public ImmutableClassIdRegistry(Map<Class<?>, Integer> classIdMapping) {
        this.classIdMapping = new HashMap<>(classIdMapping);
        this.idClassMapping = classIdMapping.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    @Override
    public Integer getId(Class<?> classInstance) {
        return classIdMapping.get(classInstance);
    }

    @Override
    public Class<?> getClass(Integer classId) {
        return idClassMapping.get(classId);
    }
}
