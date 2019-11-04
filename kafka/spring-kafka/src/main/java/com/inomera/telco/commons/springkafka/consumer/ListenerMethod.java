package com.inomera.telco.commons.springkafka.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author Serdar Kuzucu
 */
public class ListenerMethod {
    private static final Logger LOG = LoggerFactory.getLogger(ListenerMethod.class);

    private final Object listenerInstance;
    private final Method listenerMethod;

    public ListenerMethod(Object listenerInstance, Method listenerMethod) {
        this.listenerInstance = listenerInstance;
        this.listenerMethod = listenerMethod;
    }

    void invoke(Object message) {
        try {
            listenerMethod.invoke(listenerInstance, message);
        } catch (Exception e) {
            LOG.error("Error invoking listener method {} with message {}", this, message, e);
        }
    }

    @Override
    public String toString() {
        return "ListenerMethod [class=" + listenerInstance.getClass() + ", method=" + listenerMethod.getName() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ListenerMethod that = (ListenerMethod) o;
        return listenerInstance.equals(that.listenerInstance) &&
                listenerMethod.equals(that.listenerMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(listenerInstance, listenerMethod);
    }
}
