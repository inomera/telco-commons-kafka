package com.inomera.telco.commons.springkafka.consumer.invoker;

import lombok.Getter;

import java.util.Objects;

/**
 * @author Serdar Kuzucu
 */
@Getter
public class TopicBasedListenerEndpointDescriptor extends ListenerEndpointDescriptor {
    private final String topic;

    public TopicBasedListenerEndpointDescriptor(String topic, Class<?> messageType) {
        super(messageType);
        this.topic = topic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TopicBasedListenerEndpointDescriptor that = (TopicBasedListenerEndpointDescriptor) o;
        return Objects.equals(topic, that.topic) && Objects.equals(messageType, that.messageType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, messageType);
    }

    @Override
    public boolean matches(String topic, Class<?> messageType) {
        return Objects.equals(topic, this.topic) && Objects.equals(messageType, this.messageType);
    }

    @Override
    public String toString() {
        return messageType + " instances from topic " + topic;
    }
}
