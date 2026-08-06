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
import com.inomera.telco.commons.springkafka.consumer.retry.DefaultBulkRecordRetryer;
import com.inomera.telco.commons.springkafka.consumer.retry.DefaultRecordRetryer;
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
    private boolean serverSideRebalance;
    private String remoteAssignor;

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

    /**
     * Delegates rebalancing to the broker via the KIP-848 group protocol. See
     * {@link com.inomera.telco.commons.springkafka.consumer.GroupProtocol} for what changes and
     * which classic-protocol settings are dropped.
     */
    public KafkaConsumerBuilder serverSideRebalance() {
        return serverSideRebalance(null);
    }

    /**
     * Delegates rebalancing to the broker, asking for a specific server-side assignor.
     *
     * <p>The request is recorded and applied when the consumer is built, not here. Applying it
     * eagerly made the call order-sensitive: {@code properties(props)} replaces the whole
     * {@code Properties} instance, so {@code .serverSideRebalance().properties(props)} silently
     * dropped {@code group.protocol} and the consumer came up on the classic protocol with no
     * indication that anything was wrong.
     *
     * @param remoteAssignor {@code uniform} or {@code range}; {@code null} leaves the broker default
     */
    public KafkaConsumerBuilder serverSideRebalance(String remoteAssignor) {
        this.serverSideRebalance = true;
        this.remoteAssignor = remoteAssignor;
        return this;
    }

    private void applyGroupProtocolIfRequested() {
        if (serverSideRebalance) {
            GroupProtocol.enable(this.properties, remoteAssignor);
        }
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
        applyGroupProtocolIfRequested();

        final KafkaConsumerProperties properties = new KafkaConsumerProperties(groupId, topics, topicPattern,
                offsetCommitStrategy, this.properties);

        final DefaultConsumerPoller consumerPoller = new DefaultConsumerPoller(properties, valueDeserializer,
                autoPartitionPause);
        consumerPoller.setConsumerThreadFactory(getWrappedThreadFactory(consumerPoller));
        final ConsumerInvoker invoker = consumerInvokerBuilder.build(consumerPoller, groupId);
        this.inMemoryRecordConsumer = this.inMemoryRecordConsumer == null ? new DefaultInMemoryRecordRetryConsumer(invoker::invoke) : this.inMemoryRecordConsumer;
        // Let the retry consumer tell the poller when a retry is done, so the poller can keep
        // the offset withheld until then instead of committing a record that only exists in RAM.
        this.inMemoryRecordConsumer.setRetryOutcomeListener(consumerPoller::releaseWithheldOffset);
        // Wire the retryer to this consumer's own retry queue. It used to reach a static queue
        // shared by every consumer in the JVM, so retries could run against the wrong group.
        consumerPoller.setRecordRetryer(new DefaultRecordRetryer(this.inMemoryRecordConsumer));
        return new SmartKafkaMessageConsumer(consumerPoller, invoker, this.inMemoryRecordConsumer);
    }

    public KafkaMessageConsumer buildBulk() {
        Assert.hasText(groupId, "groupId is null or empty");
        Assert.notNull(offsetCommitStrategy, "offsetCommitStrategy is null");
        Assert.notNull(properties, "properties is null");
        Assert.notNull(valueDeserializer, "valueDeserializer is null");
        applyGroupProtocolIfRequested();

        final KafkaConsumerProperties properties = new KafkaConsumerProperties(groupId, topics, topicPattern,
                offsetCommitStrategy, this.properties);

        final BulkConsumerPoller bulkConsumerPoller = new BulkConsumerPoller(properties, valueDeserializer,
                autoPartitionPause);
        bulkConsumerPoller.setConsumerThreadFactory(getWrappedThreadFactory(bulkConsumerPoller));
        final BulkConsumerInvoker bulkConsumerInvoker = consumerInvokerBuilder.buildBulk(bulkConsumerPoller, groupId);
        this.inMemoryBulkRecordRetryConsumer = this.inMemoryBulkRecordRetryConsumer == null ? new DefaultInMemoryBulkRecordRetryConsumer(bulkConsumerInvoker::invoke) : this.inMemoryBulkRecordRetryConsumer;
        this.inMemoryBulkRecordRetryConsumer.setRetryOutcomeListener(bulkConsumerPoller::releaseWithheldOffset);
        bulkConsumerPoller.setBulkRecordRetryer(new DefaultBulkRecordRetryer(this.inMemoryBulkRecordRetryConsumer));
        return new BulkSmartKafkaMessageConsumer(bulkConsumerPoller, bulkConsumerInvoker, this.inMemoryBulkRecordRetryConsumer);
    }
}
