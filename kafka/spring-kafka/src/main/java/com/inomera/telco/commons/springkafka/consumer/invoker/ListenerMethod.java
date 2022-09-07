package com.inomera.telco.commons.springkafka.consumer.invoker;

import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Serdar Kuzucu
 */
public class ListenerMethod {
    private static final Logger LOG = LoggerFactory.getLogger(ListenerMethod.class);
    private final ConcurrentMap<String, KafkaListener> RETRYABLE_CONSUMER_MESSAGES = new ConcurrentHashMap<>();
    private final Object listenerInstance;
    private final Method listenerMethod;

    public ListenerMethod(Object listenerInstance, Method listenerMethod) {
        this.listenerInstance = listenerInstance;
        this.listenerMethod = listenerMethod;
    }

    KafkaListener invoke(Object message, String topic) {
        try {
            listenerMethod.invoke(listenerInstance, message);
            return null;
        } catch (InvocationTargetException ite) {
            LOG.debug("InvocationTargetException listener method {} with message {}, topic {}", this, message, topic, ite);
            return getKafkaListener();
        } catch (Exception e) {
            LOG.error("Error invoking listener method {} with message {}", this, message, e);
            return null;
        }
    }

    private KafkaListener getKafkaListener() {
        final String key = listenerInstance.getClass().getName() + "-" + listenerMethod.getName();
        KafkaListener annotation = RETRYABLE_CONSUMER_MESSAGES.get(key);
        if (annotation == null) {
            return getAndPutKafkaListener(key);
        }
        return annotation;
    }

    private KafkaListener getAndPutKafkaListener(String key) {
        final KafkaListener annotation = AnnotationUtils.findAnnotation(listenerMethod, KafkaListener.class);
        if (annotation == null) {
            return null;
        }
        RETRYABLE_CONSUMER_MESSAGES.putIfAbsent(key, annotation);
        return annotation;
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
