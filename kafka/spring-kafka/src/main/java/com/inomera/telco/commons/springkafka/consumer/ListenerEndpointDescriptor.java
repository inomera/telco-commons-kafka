package com.inomera.telco.commons.springkafka.consumer;

import java.util.Objects;

/**
 * @author Serdar Kuzucu
 */
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ListenerEndpointDescriptor [");
        sb.append("topic=").append(topic);
        sb.append(", groupId=").append(groupId);
        sb.append(", messageType=").append(messageType);
        sb.append("]");
        return sb.toString();
    }
}
