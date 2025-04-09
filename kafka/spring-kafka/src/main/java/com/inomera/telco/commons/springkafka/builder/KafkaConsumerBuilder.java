package com.inomera.telco.commons.springkafka.builder;

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
 * @author Serdar Kuzucu
 * @author Turgay Can
 */
public class KafkaConsumerBuilder {

    private final ConsumerInvokerBuilder consumerInvokerBuilder;

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

    private KafkaConsumerBuilder(ListenerMethodRegistry listenerMethodRegistry) {
        Assert.notNull(listenerMethodRegistry, "listenerMethodRegistry is null");
        this.consumerInvokerBuilder = new ConsumerInvokerBuilder(this, listenerMethodRegistry);
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

    public KafkaConsumerBuilder threadStore(ConsumerThreadStore threadStore) {
        Assert.notNull(threadStore, "threadStore cannot be null");
        this.threadStore = threadStore;
        return this;
    }


    public KafkaConsumerBuilder inMemoryBulkRecordRetryConsumer(InMemoryBulkRecordRetryConsumer inMemoryBulkRecordRetryConsumer) {
        this.inMemoryBulkRecordRetryConsumer = inMemoryBulkRecordRetryConsumer;
        return this;
    }

    public KafkaConsumerBuilder inMemoryRecordConsumer(InMemoryRecordRetryConsumer inMemoryRecordConsumer) {
        this.inMemoryRecordConsumer = inMemoryRecordConsumer;
        return this;
    }

    public ConsumerInvokerBuilder invoker() {
        return consumerInvokerBuilder;
    }

    public KafkaConsumerBuilder autoPartitionPause(boolean autoPartitionPause) {
        this.autoPartitionPause = autoPartitionPause;
        return this;
    }

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
        final ConsumerInvoker invoker = consumerInvokerBuilder.build(consumerPoller, groupId);
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
        final BulkConsumerInvoker bulkConsumerInvoker = consumerInvokerBuilder.buildBulk(bulkConsumerPoller, groupId);
        this.inMemoryBulkRecordRetryConsumer = this.inMemoryBulkRecordRetryConsumer == null ? new DefaultInMemoryBulkRecordRetryConsumer(bulkConsumerInvoker::invoke) : this.inMemoryBulkRecordRetryConsumer;
        return new BulkSmartKafkaMessageConsumer(bulkConsumerPoller, bulkConsumerInvoker, this.inMemoryBulkRecordRetryConsumer);
    }
}
