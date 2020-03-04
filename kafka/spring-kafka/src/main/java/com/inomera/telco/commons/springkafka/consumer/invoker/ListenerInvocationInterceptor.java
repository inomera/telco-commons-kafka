package com.inomera.telco.commons.springkafka.consumer.invoker;

/**
 * @author Serdar Kuzucu
 */
public interface ListenerInvocationInterceptor {
    default void beforeInvocation(Object message) {
    }

    default void afterInvocation(Object message) {
    }
}
