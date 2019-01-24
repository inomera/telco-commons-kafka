package com.inomera.telco.commons.springkafka.consumer;

import com.inomera.telco.commons.lang.Assert;
import com.inomera.telco.commons.lang.thread.IncrementalNamingThreadFactory;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;

/**
 * @author Serdar Kuzucu
 */
public class KafkaConsumerBuilder {
    private ListenerMethodRegistry listenerMethodRegistry;
    private String groupId;
    private List<String> topics = new ArrayList<>();
    private Pattern topicPattern;
    private OffsetCommitStrategy offsetCommitStrategy;
    private Properties properties = new Properties();
    private int numberOfThreads = 1;
    private Deserializer<?> valueDeserializer;
    private ThreadFactory consumerThreadFactory;
    private ThreadFactory invokerThreadFactory;
    private List<ListenerInvocationInterceptor> interceptors = new ArrayList<>();

    private KafkaConsumerBuilder(ListenerMethodRegistry listenerMethodRegistry) {
        Assert.notNull(listenerMethodRegistry, "listenerMethodRegistry is null");
        this.listenerMethodRegistry = listenerMethodRegistry;
    }

    public static KafkaConsumerBuilder builder(ListenerMethodRegistry listenerMethodRegistry) {
        Assert.notNull(listenerMethodRegistry, "listenerMethodRegistry is null");
        return new KafkaConsumerBuilder(listenerMethodRegistry);
    }

    public KafkaConsumerBuilder groupId(String groupId) {
        Assert.hasText(groupId, "groupId is null or empty");
        this.groupId = groupId;
        return this;
    }

    public KafkaConsumerBuilder interceptor(ListenerInvocationInterceptor interceptor) {
        Assert.notNull(interceptor, "interceptor is null");
        this.interceptors.add(interceptor);
        return this;
    }

    public KafkaConsumerBuilder interceptors(Collection<ListenerInvocationInterceptor> interceptors) {
        Assert.notNull(interceptors, "interceptors is null");
        this.interceptors.addAll(interceptors);
        return this;
    }

    public KafkaConsumerBuilder topics(Collection<String> topics) {
        Assert.notNull(topics, "topics is null");
        this.topics.addAll(topics);
        this.topicPattern = null;
        return this;
    }

    public KafkaConsumerBuilder topics(String... topics) {
        Assert.notNull(topics, "topics is null");
        this.topics.addAll(Arrays.asList(topics));
        this.topicPattern = null;
        return this;
    }

    public KafkaConsumerBuilder topicPattern(Pattern topicPattern) {
        Assert.notNull(topicPattern, "topicPattern is null");
        this.topicPattern = topicPattern;
        this.topics = new ArrayList<>();
        return this;
    }

    public KafkaConsumerBuilder offsetCommitStrategy(OffsetCommitStrategy offsetCommitStrategy) {
        Assert.notNull(offsetCommitStrategy, "offsetCommitStrategy is null");
        this.offsetCommitStrategy = offsetCommitStrategy;
        return this;
    }

    public KafkaConsumerBuilder numberOfThreads(int numberOfThreads) {
        Assert.isTrue(numberOfThreads > 0, "numberOfThreads is smaller than 1");
        this.numberOfThreads = numberOfThreads;
        return this;
    }

    public KafkaConsumerBuilder properties(Properties properties) {
        Assert.notNull(properties, "properties is null");
        this.properties = properties;
        return this;
    }

    public KafkaConsumerBuilder properties(String key, String value) {
        Assert.notNull(key, "key is null");
        Assert.notNull(value, "value is null");
        this.properties.setProperty(key, value);
        return this;
    }

    public KafkaConsumerBuilder valueDeserializer(Deserializer<?> valueDeserializer) {
        Assert.notNull(valueDeserializer, "valueDeserializer is null");
        this.valueDeserializer = valueDeserializer;
        return this;
    }

    public KafkaConsumerBuilder consumerThreadFactory(ThreadFactory consumerThreadFactory) {
        Assert.notNull(consumerThreadFactory, "consumerThreadFactory cannot be null");
        this.consumerThreadFactory = consumerThreadFactory;
        return this;
    }

    public KafkaConsumerBuilder invokerThreadFactory(ThreadFactory invokerThreadFactory) {
        Assert.notNull(invokerThreadFactory, "invokerThreadFactory cannot be null");
        this.invokerThreadFactory = invokerThreadFactory;
        return this;
    }

    private ThreadFactory getOrCreateConsumerThreadFactory() {
        if (this.consumerThreadFactory != null) {
            return this.consumerThreadFactory;
        }

        return new IncrementalNamingThreadFactory("consumer-" + groupId + "-");
    }

    private ThreadFactory getOrCreateInvokerThreadFactory() {
        if (this.invokerThreadFactory != null) {
            return this.invokerThreadFactory;
        }

        return new IncrementalNamingThreadFactory("consumer-invoker-" + groupId + "-");
    }

    public KafkaMessageConsumer build() {
        Assert.hasText(groupId, "groupId is null or empty");
        Assert.notNull(offsetCommitStrategy, "offsetCommitStrategy is null");
        Assert.notNull(properties, "properties is null");
        Assert.notNull(valueDeserializer, "valueDeserializer is null");
        Assert.isTrue(numberOfThreads > 0, "numberOfThreads is smaller than 1");

        final KafkaConsumerProperties properties = new KafkaConsumerProperties(groupId, topics, topicPattern,
                offsetCommitStrategy, this.properties);

        return new KafkaMessageConsumer(properties, listenerMethodRegistry, interceptors,
                getOrCreateConsumerThreadFactory(), getOrCreateInvokerThreadFactory(), valueDeserializer,
                numberOfThreads);
    }
}
