package com.inomera.telco.commons.springkafka.consumer.invoker;

import java.util.Objects;

/**
 * @author Serdar Kuzucu
 */
public class AllTopicsListenerEndpointDescriptor extends ListenerEndpointDescriptor {
    public AllTopicsListenerEndpointDescriptor(Class<?> messageType) {
        super(messageType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AllTopicsListenerEndpointDescriptor that = (AllTopicsListenerEndpointDescriptor) o;
        return Objects.equals(messageType, that.messageType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageType);
    }

    @Override
    public boolean matches(String topic, Class<?> messageType) {
        return Objects.equals(messageType, this.messageType);
    }

    @Override
    public String toString() {
        return messageType + " instances from all topics";
    }
}
