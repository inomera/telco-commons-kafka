package com.inomera.telco.commons.springkafka.consumer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Serdar Kuzucu
 */
public class ListenerMethodRegistry {
    private static final Set<ListenerMethod> EMPTY = Collections.emptySet();

    private final Map<ListenerEndpointDescriptor, Set<ListenerMethod>> listenerMethodMap = new ConcurrentHashMap<>();
    private final Map<SuperClassListenerEndpointDescriptor, Set<ListenerMethod>> superClassListenerMethodMap = new ConcurrentHashMap<>();

    public void addListenerMethod(ListenerEndpointDescriptor listenerEndpointDescriptor,
                                  ListenerMethod listenerMethod) {
        addToSetInMap(this.listenerMethodMap, listenerEndpointDescriptor, listenerMethod);
    }

    public void addListenerMethod(SuperClassListenerEndpointDescriptor listenerEndpointDescriptor,
                                  ListenerMethod listenerMethod) {
        addToSetInMap(this.superClassListenerMethodMap, listenerEndpointDescriptor, listenerMethod);
    }

    Collection<ListenerMethod> getListenerMethods(ListenerEndpointDescriptor listenerEndpointDescriptor) {
        final Stream<ListenerMethod> concreteListeners = this.listenerMethodMap
                .getOrDefault(listenerEndpointDescriptor, EMPTY)
                .stream();
        final Stream<ListenerMethod> superListeners = this.superClassListenerMethodMap
                .entrySet()
                .stream()
                .filter(e -> e.getKey().matches(listenerEndpointDescriptor))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream);
        return Stream.concat(concreteListeners, superListeners).collect(Collectors.toSet());
    }

    private <K, V> void addToSetInMap(Map<K, Set<V>> map, K key, V value) {
        map.computeIfAbsent(key, k -> new CopyOnWriteArraySet<>()).add(value);
    }
}
