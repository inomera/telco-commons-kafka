package com.inomera.telco.commons.kafkaprotobuf;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Parser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ImmutableClassIdRegistry implements ClassIdRegistry {
    private final Map<Class<? extends GeneratedMessageV3>, Integer> classIdMapping;
    private final Map<Integer, Class<? extends GeneratedMessageV3>> idClassMapping;
    private final Map<Integer, Parser<? extends GeneratedMessageV3>> idParserMapping;

    public ImmutableClassIdRegistry(Map<Class<? extends GeneratedMessageV3>, Integer> classIdMapping) {
        this.classIdMapping = new HashMap<>(classIdMapping);
        this.idClassMapping = classIdMapping.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        this.idParserMapping = idClassMapping.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> getParser(entry.getValue())));
    }

    @Override
    public Integer getId(Class<? extends GeneratedMessageV3> classInstance) {
        return classIdMapping.get(classInstance);
    }

    @Override
    public Class<? extends GeneratedMessageV3> getClass(Integer classId) {
        return idClassMapping.get(classId);
    }

    @Override
    public Parser<? extends GeneratedMessageV3> getParser(Integer classId) {
        return idParserMapping.get(classId);
    }

    private static Parser<? extends GeneratedMessageV3> getParser(Class<? extends GeneratedMessageV3> protoClass) {
        try {
            final GeneratedMessageV3 defaultInstance = getDefaultInstance(protoClass);
            return defaultInstance.getParserForType();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("NoSuchMethodException: Class " + protoClass.getName()
                    + " does not have 'getParserForType()' method");
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("InvocationTargetException: Cannot invoke 'getParserForType()' " +
                    "method on class " + protoClass.getName());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("IllegalAccessException: Cannot invoke 'getParserForType()' " +
                    "method on class " + protoClass.getName());
        }
    }

    private static GeneratedMessageV3 getDefaultInstance(Class<? extends GeneratedMessageV3> protoClass)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Method methodGetDefaultInstance = protoClass.getMethod("getDefaultInstance");
        return (GeneratedMessageV3) methodGetDefaultInstance.invoke(null);
    }
}
