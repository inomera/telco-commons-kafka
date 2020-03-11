package com.inomera.telco.commons.springkafka.consumer.invoker;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Serdar Kuzucu
 */
@Getter
@RequiredArgsConstructor
public abstract class ListenerEndpointDescriptor {
    protected final Class<?> messageType;

    public abstract boolean matches(String topic, Class<?> messageType);

    protected boolean isAssignableFrom(Class<?> messageType) {
        return this.messageType.isAssignableFrom(messageType);
    }
}
