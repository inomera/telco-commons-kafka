package com.inomera.telco.commons.springkafka.consumer.invoker;

import java.util.Objects;

/**
 * @author Serdar Kuzucu
 */
public class AllTopicsSuperClassListenerEndpointDescriptor extends ListenerEndpointDescriptor {
    public AllTopicsSuperClassListenerEndpointDescriptor(Class<?> messageType) {
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
        final AllTopicsSuperClassListenerEndpointDescriptor that = (AllTopicsSuperClassListenerEndpointDescriptor) o;
        return Objects.equals(messageType, that.messageType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageType);
    }

    @Override
    public boolean matches(String topic, Class<?> messageType) {
        return isAssignableFrom(messageType);
    }

    @Override
    public String toString() {
        return "Child classes of " + messageType + " from all topics";
    }
}
