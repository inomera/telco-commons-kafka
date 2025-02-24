package com.inomera.telco.commons.springkafka.builder.virtual;

import com.inomera.telco.commons.lang.Assert;
import com.inomera.telco.commons.lang.thread.IncrementalNamingThreadFactory;
import com.inomera.telco.commons.springkafka.consumer.*;
import com.inomera.telco.commons.springkafka.consumer.invoker.BulkConsumerInvoker;
import com.inomera.telco.commons.springkafka.consumer.invoker.ConsumerInvoker;
import com.inomera.telco.commons.springkafka.consumer.invoker.ListenerMethodRegistry;
import com.inomera.telco.commons.springkafka.consumer.poller.BulkConsumerPoller;
import com.inomera.telco.commons.springkafka.consumer.poller.ConsumerPoller;
import com.inomera.telco.commons.springkafka.consumer.poller.DefaultConsumerPoller;
import com.inomera.telco.commons.springkafka.consumer.retry.DefaultInMemoryBulkRecordRetryConsumer;
import com.inomera.telco.commons.springkafka.consumer.retry.DefaultInMemoryRecordRetryConsumer;
import com.inomera.telco.commons.springkafka.consumer.retry.InMemoryBulkRecordRetryConsumer;
import com.inomera.telco.commons.springkafka.consumer.retry.InMemoryRecordRetryConsumer;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;

import static com.inomera.telco.commons.springkafka.SpringKafkaConstants.CONSUMER_POLLER_THREAD_NAME_FORMAT;

/**
 * @author Turgay Can
 */
public class VirtualKafkaConsumerBuilder {
    private final VirtualConsumerInvokerBuilder virtualConsumerInvokerBuilder;

    private String groupId;
    private List<String> topics = new ArrayList<>();
    private Pattern topicPattern;
    private OffsetCommitStrategy offsetCommitStrategy;
    private Properties properties = new Properties();
    private Deserializer<?> valueDeserializer;
    private ThreadFactory consumerThreadFactory;
    private boolean autoPartitionPause = true;
    private ConsumerThreadStore threadStore;
    private InMemoryBulkRecordRetryConsumer inMemoryBulkRecordRetryConsumer;
    private InMemoryRecordRetryConsumer inMemoryRecordConsumer;

    private VirtualKafkaConsumerBuilder(ListenerMethodRegistry listenerMethodRegistry) {
        Assert.notNull(listenerMethodRegistry, "listenerMethodRegistry is null");
        this.virtualConsumerInvokerBuilder = new VirtualConsumerInvokerBuilder(this, listenerMethodRegistry);
    }

    public static VirtualKafkaConsumerBuilder builder(ListenerMethodRegistry listenerMethodRegistry) {
        Assert.notNull(listenerMethodRegistry, "listenerMethodRegistry is null");
        return new VirtualKafkaConsumerBuilder(listenerMethodRegistry);
    }

    public VirtualKafkaConsumerBuilder groupId(String groupId) {
        Assert.hasText(groupId, "groupId is null or empty");
        this.groupId = groupId;
        return this;
    }

    public VirtualKafkaConsumerBuilder topics(Collection<String> topics) {
        Assert.notNull(topics, "topics is null");
        this.topics.addAll(topics);
        this.topicPattern = null;
        return this;
    }

    public VirtualKafkaConsumerBuilder topics(String... topics) {
        Assert.notNull(topics, "topics is null");
        this.topics.addAll(Arrays.asList(topics));
        this.topicPattern = null;
        return this;
    }

    public VirtualKafkaConsumerBuilder topicPattern(Pattern topicPattern) {
        Assert.notNull(topicPattern, "topicPattern is null");
        this.topicPattern = topicPattern;
        this.topics = new ArrayList<>();
        return this;
    }

    public VirtualKafkaConsumerBuilder offsetCommitStrategy(OffsetCommitStrategy offsetCommitStrategy) {
        Assert.notNull(offsetCommitStrategy, "offsetCommitStrategy is null");
        this.offsetCommitStrategy = offsetCommitStrategy;
        return this;
    }

    public VirtualKafkaConsumerBuilder properties(Properties properties) {
        Assert.notNull(properties, "properties is null");
        this.properties = properties;
        return this;
    }

    public VirtualKafkaConsumerBuilder properties(String key, String value) {
        Assert.notNull(key, "key is null");
        Assert.notNull(value, "value is null");
        this.properties.setProperty(key, value);
        return this;
    }

    public VirtualKafkaConsumerBuilder valueDeserializer(Deserializer<?> valueDeserializer) {
        Assert.notNull(valueDeserializer, "valueDeserializer is null");
        this.valueDeserializer = valueDeserializer;
        return this;
    }

    public VirtualKafkaConsumerBuilder consumerThreadFactory(ThreadFactory consumerThreadFactory) {
        Assert.notNull(consumerThreadFactory, "consumerThreadFactory cannot be null");
        this.consumerThreadFactory = consumerThreadFactory;
        return this;
    }

    public VirtualKafkaConsumerBuilder threadStore(ConsumerThreadStore threadStore) {
        Assert.notNull(threadStore, "threadStore cannot be null");
        this.threadStore = threadStore;
        return this;
    }


    public VirtualKafkaConsumerBuilder inMemoryBulkRecordRetryConsumer(InMemoryBulkRecordRetryConsumer inMemoryBulkRecordRetryConsumer) {
        this.inMemoryBulkRecordRetryConsumer = inMemoryBulkRecordRetryConsumer;
        return this;
    }

    public VirtualKafkaConsumerBuilder inMemoryRecordConsumer(InMemoryRecordRetryConsumer inMemoryRecordConsumer) {
        this.inMemoryRecordConsumer = inMemoryRecordConsumer;
        return this;
    }

    public VirtualConsumerInvokerBuilder invoker() {
        return virtualConsumerInvokerBuilder;
    }

    public VirtualKafkaConsumerBuilder autoPartitionPause(boolean autoPartitionPause) {
        this.autoPartitionPause = autoPartitionPause;
        return this;
    }

    /**
     * Returns the existing consumer thread factory if present; otherwise, creates a new one with a naming
     * strategy based on the consumer poller group ID.
     * The thread should be OS Thread!! Because it is kafka consumer thread factory!!!
     * @return a ThreadFactory instance for creating consumer threads, either existing or newly created
     */
    private ThreadFactory getOrCreateConsumerThreadFactory() {
        return Objects.requireNonNullElseGet(this.consumerThreadFactory, () -> new IncrementalNamingThreadFactory(String.format(CONSUMER_POLLER_THREAD_NAME_FORMAT, groupId)));
    }

    private ConsumerThreadStore getOrCreateConsumerThreadStore() {
        return Objects.requireNonNullElseGet(this.threadStore, NoopConsumerThreadStore::new);
    }

    private ThreadFactory getWrappedThreadFactory(ConsumerPoller consumerPoller) {
        final ConsumerThreadStore threadStore = getOrCreateConsumerThreadStore();
        final ThreadFactory threadFactory = getOrCreateConsumerThreadFactory();
        return r -> {
            final Thread thread = threadFactory.newThread(r);
            threadStore.put(consumerPoller, thread);
            return thread;
        };
    }

    public KafkaMessageConsumer build() {
        Assert.hasText(groupId, "groupId is null or empty");
        Assert.notNull(offsetCommitStrategy, "offsetCommitStrategy is null");
        Assert.notNull(properties, "properties is null");
        Assert.notNull(valueDeserializer, "valueDeserializer is null");

        final KafkaConsumerProperties properties = new KafkaConsumerProperties(groupId, topics, topicPattern,
                offsetCommitStrategy, this.properties);

        final DefaultConsumerPoller consumerPoller = new DefaultConsumerPoller(properties, valueDeserializer,
                autoPartitionPause);
        consumerPoller.setConsumerThreadFactory(getWrappedThreadFactory(consumerPoller));
        final ConsumerInvoker invoker = virtualConsumerInvokerBuilder.build(consumerPoller, groupId);
        this.inMemoryRecordConsumer = this.inMemoryRecordConsumer == null ? new DefaultInMemoryRecordRetryConsumer(invoker::invoke) : this.inMemoryRecordConsumer;
        return new SmartKafkaMessageConsumer(consumerPoller, invoker, this.inMemoryRecordConsumer);
    }

    public KafkaMessageConsumer buildBulk() {
        Assert.hasText(groupId, "groupId is null or empty");
        Assert.notNull(offsetCommitStrategy, "offsetCommitStrategy is null");
        Assert.notNull(properties, "properties is null");
        Assert.notNull(valueDeserializer, "valueDeserializer is null");

        final KafkaConsumerProperties properties = new KafkaConsumerProperties(groupId, topics, topicPattern,
                offsetCommitStrategy, this.properties);

        final BulkConsumerPoller bulkConsumerPoller = new BulkConsumerPoller(properties, valueDeserializer,
                autoPartitionPause);
        bulkConsumerPoller.setConsumerThreadFactory(getWrappedThreadFactory(bulkConsumerPoller));
        final BulkConsumerInvoker bulkConsumerInvoker = virtualConsumerInvokerBuilder.buildBulk(bulkConsumerPoller, groupId);
        this.inMemoryBulkRecordRetryConsumer = this.inMemoryBulkRecordRetryConsumer == null ? new DefaultInMemoryBulkRecordRetryConsumer(bulkConsumerInvoker::invoke) : this.inMemoryBulkRecordRetryConsumer;
        return new BulkSmartKafkaMessageConsumer(bulkConsumerPoller, bulkConsumerInvoker, this.inMemoryBulkRecordRetryConsumer);
    }
}
