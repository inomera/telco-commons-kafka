package com.inomera.telco.commons.springkafka.consumer.invoker;

import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * @author Serdar Kuzucu
 */
@Getter
@ToString
public class ListenerEndpointDescriptor {
    private final String topic;
    private final String groupId;
    private final Class<?> messageType;

    public ListenerEndpointDescriptor(String topic, String groupId, Class<?> messageType) {
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
        final ListenerEndpointDescriptor that = (ListenerEndpointDescriptor) o;
        return Objects.equals(topic, that.topic) &&
                Objects.equals(groupId, that.groupId) &&
                Objects.equals(messageType, that.messageType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, groupId, messageType);
    }
}
