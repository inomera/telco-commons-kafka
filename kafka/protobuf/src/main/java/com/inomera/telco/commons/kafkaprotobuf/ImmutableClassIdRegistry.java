package com.inomera.telco.commons.kafkaprotobuf;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Parser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ImmutableClassIdRegistry implements ClassIdRegistry {
    private final Map<Class<? extends GeneratedMessage>, Integer> classIdMapping;
    private final Map<Integer, Class<? extends GeneratedMessage>> idClassMapping;
    private final Map<Integer, Parser<? extends GeneratedMessage>> idParserMapping;

    public ImmutableClassIdRegistry(Map<Class<? extends GeneratedMessage>, Integer> classIdMapping) {
        this.classIdMapping = new HashMap<>(classIdMapping);
        this.idClassMapping = classIdMapping.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        this.idParserMapping = idClassMapping.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> getParser(entry.getValue())));
    }

    @Override
    public Integer getId(Class<? extends GeneratedMessage> classInstance) {
        return classIdMapping.get(classInstance);
    }

    @Override
    public Class<? extends GeneratedMessage> getClass(Integer classId) {
        return idClassMapping.get(classId);
    }

    @Override
    public Parser<? extends GeneratedMessage> getParser(Integer classId) {
        return idParserMapping.get(classId);
    }

    private static Parser<? extends GeneratedMessage> getParser(Class<? extends GeneratedMessage> protoClass) {
        try {
            final GeneratedMessage defaultInstance = getDefaultInstance(protoClass);
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

    private static GeneratedMessage getDefaultInstance(Class<? extends GeneratedMessage> protoClass)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Method methodGetDefaultInstance = protoClass.getMethod("getDefaultInstance");
        return (GeneratedMessage) methodGetDefaultInstance.invoke(null);
    }
}
