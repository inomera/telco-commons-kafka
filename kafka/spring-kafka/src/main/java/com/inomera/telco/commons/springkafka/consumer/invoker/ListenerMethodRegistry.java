package com.inomera.telco.commons.springkafka.consumer.invoker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
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

    /** Memoized (groupId, topic, messageType) -> listeners, rebuilt on registration. */
    private final Map<ResolutionKey, ListenerMethod[]> resolutionCache = new ConcurrentHashMap<>();

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
        // Registration happens once at startup, but invalidate anyway so a late registration
        // cannot be masked by a stale cache entry.
        resolutionCache.clear();
        LOG.info("Registered: {} {} {}", groupId, endpointDescriptor, listenerMethod);
    }

    /**
     * Resolves the listeners for a dispatch key, memoized.
     *
     * <p>This used to rebuild a stream and linearly scan every registered endpoint descriptor
     * for <em>every record</em>. Registrations only happen during startup, so the mapping is
     * effectively immutable and worth caching.
     */
    ListenerMethod[] resolveListenerMethods(String groupId, String topic, Class<?> messageType) {
        return resolutionCache.computeIfAbsent(new ResolutionKey(groupId, topic, messageType),
                key -> lookup(key.groupId(), key.topic(), key.messageType()));
    }

    Stream<ListenerMethod> getListenerMethods(String groupId, String topic, Class<?> messageType) {
        return Arrays.stream(resolveListenerMethods(groupId, topic, messageType));
    }

    private ListenerMethod[] lookup(String groupId, String topic, Class<?> messageType) {
        return listenerMap.getOrDefault(groupId, EMPTY_MAP)
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().matches(topic, messageType))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .toArray(ListenerMethod[]::new);
    }

    private record ResolutionKey(String groupId, String topic, Class<?> messageType) {
    }

    private Map<ListenerEndpointDescriptor, Set<ListenerMethod>> getOrCreateListenerEndpointDescriptorMap(String groupId) {
        return listenerMap.computeIfAbsent(groupId, k -> new ConcurrentHashMap<>());
    }

    private Set<ListenerMethod> getOrCreateListenerMethodSet(String groupId, ListenerEndpointDescriptor endpointDescriptor) {
        return getOrCreateListenerEndpointDescriptorMap(groupId)
                .computeIfAbsent(endpointDescriptor, k -> new CopyOnWriteArraySet<>());
    }
}
