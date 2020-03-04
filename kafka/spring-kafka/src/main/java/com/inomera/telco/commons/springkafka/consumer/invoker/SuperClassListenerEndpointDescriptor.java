package com.inomera.telco.commons.springkafka.consumer.invoker;

import lombok.ToString;

import java.util.Objects;

/**
 * @author Serdar Kuzucu
 */
@ToString
public class SuperClassListenerEndpointDescriptor {
    private final String topic;
    private final String groupId;
    private final Class<?> messageType;

    public SuperClassListenerEndpointDescriptor(String topic, String groupId, Class<?> messageType) {
        this.topic = topic;
        this.groupId = groupId;
        this.messageType = messageType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SuperClassListenerEndpointDescriptor that = (SuperClassListenerEndpointDescriptor) o;
        return Objects.equals(topic, that.topic) &&
                Objects.equals(groupId, that.groupId) &&
                Objects.equals(messageType, that.messageType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, groupId, messageType);
    }

    boolean matches(ListenerEndpointDescriptor descriptor) {
        return Objects.equals(descriptor.getTopic(), topic) &&
                Objects.equals(descriptor.getGroupId(), groupId) &&
                isAssignableFrom(descriptor.getMessageType());
    }

    private boolean isAssignableFrom(Class<?> messageType) {
        return this.messageType.isAssignableFrom(messageType);
    }
}
