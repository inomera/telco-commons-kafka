package com.inomera.telco.commons.springkafka.consumer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author Serdar Kuzucu
 */
public class ListenerMethodRegistry {
    @SuppressWarnings("unchecked")
    private static final Set<ListenerMethod> EMPTY = Collections.EMPTY_SET;

    private final Map<ListenerEndpointDescriptor, Set<ListenerMethod>> listenerMethodMap = new ConcurrentHashMap<>();

    public void addListenerMethod(ListenerEndpointDescriptor listenerEndpointDescriptor,
                                  ListenerMethod listenerMethod) {
        final Set<ListenerMethod> listenerMethodSet = this.listenerMethodMap
                .computeIfAbsent(listenerEndpointDescriptor, m -> new CopyOnWriteArraySet<>());
        listenerMethodSet.add(listenerMethod);
    }

    Collection<ListenerMethod> getListenerMethods(ListenerEndpointDescriptor listenerEndpointDescriptor) {
        return this.listenerMethodMap.getOrDefault(listenerEndpointDescriptor, EMPTY);
    }
}
