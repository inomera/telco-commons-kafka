package com.inomera.telco.commons.springkafka.consumer.invoker;

import org.apache.kafka.common.header.Headers;

/**
 * @author Serdar Kuzucu
 */
public interface ListenerInvocationInterceptor {
    default void beforeInvocation(Object message, Headers headers) {
    }

    default void afterInvocation(Object message, Headers headers) {
    }
}
