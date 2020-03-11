package com.inomera.telco.commons.springkafka.consumer.invoker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

/**
 * @author Serdar Kuzucu
 */
public class ListenerMethodRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(ListenerMethodRegistry.class);

    private static final Map<ListenerEndpointDescriptor, Set<ListenerMethod>> EMPTY_MAP = Collections.emptyMap();
    private final Map<String, Map<ListenerEndpointDescriptor, Set<ListenerMethod>>> listenerMap = new ConcurrentHashMap<>();

    public void addParentTypeListenerMethod(String groupId, String topic, Class<?> messageType, ListenerMethod listenerMethod) {
        final ListenerEndpointDescriptor endpointDescriptor = new TopicBasedSuperClassListenerEndpointDescriptor(topic, messageType);
        register(groupId, endpointDescriptor, listenerMethod);
    }

    public void addListenerMethod(String groupId, String topic, Class<?> messageType, ListenerMethod listenerMethod) {
        final ListenerEndpointDescriptor endpointDescriptor = new TopicBasedListenerEndpointDescriptor(topic, messageType);
        register(groupId, endpointDescriptor, listenerMethod);
    }

    public void addParentTypeListenerMethod(String groupId, Class<?> messageType, ListenerMethod listenerMethod) {
        final ListenerEndpointDescriptor endpointDescriptor = new AllTopicsSuperClassListenerEndpointDescriptor(messageType);
        register(groupId, endpointDescriptor, listenerMethod);
    }

    public void addListenerMethod(String groupId, Class<?> messageType, ListenerMethod listenerMethod) {
        final ListenerEndpointDescriptor endpointDescriptor = new AllTopicsListenerEndpointDescriptor(messageType);
        register(groupId, endpointDescriptor, listenerMethod);
    }

    private void register(String groupId, ListenerEndpointDescriptor endpointDescriptor, ListenerMethod listenerMethod) {
        getOrCreateListenerMethodSet(groupId, endpointDescriptor).add(listenerMethod);
        LOG.info("Registered: {} {} {}", groupId, endpointDescriptor, listenerMethod);
    }

    Stream<ListenerMethod> getListenerMethods(String groupId, String topic, Class<?> messageType) {
        return listenerMap.getOrDefault(groupId, EMPTY_MAP)
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().matches(topic, messageType))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream);
    }

    private Map<ListenerEndpointDescriptor, Set<ListenerMethod>> getOrCreateListenerEndpointDescriptorMap(String groupId) {
        return listenerMap.computeIfAbsent(groupId, k -> new ConcurrentHashMap<>());
    }

    private Set<ListenerMethod> getOrCreateListenerMethodSet(String groupId, ListenerEndpointDescriptor endpointDescriptor) {
        return getOrCreateListenerEndpointDescriptorMap(groupId)
                .computeIfAbsent(endpointDescriptor, k -> new CopyOnWriteArraySet<>());
    }
}
