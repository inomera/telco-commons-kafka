package com.inomera.telco.commons.springkafka.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiPredicate;

public class DefaultKafkaProducerFactory<V> implements ProducerFactory<V>, DisposableBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultKafkaProducerFactory.class);

    private final ReentrantLock globalLock = new ReentrantLock();
    private final Properties properties;
    private final Serializer<V> valueSerializer;
    private final Serializer<String> keySerializer;
    private final AtomicInteger epoch = new AtomicInteger();
    private final Map<String, BlockingQueue<CloseSafeProducer<String, V>>> cache = new ConcurrentHashMap<>();

    private TransactionIdSuffixStrategy transactionIdSuffixStrategy = new DefaultTransactionIdSuffixStrategy(0);
    private Duration physicalCloseTimeout = DEFAULT_PHYSICAL_CLOSE_TIMEOUT;

    private volatile String transactionIdPrefix;
    private long maxAge;

    public DefaultKafkaProducerFactory(Serializer<V> valueSerializer, Properties properties) {
        this.keySerializer = new StringSerializer();
        this.valueSerializer = valueSerializer;
        this.properties = properties;

        String txId = (String) properties.get(ProducerConfig.TRANSACTIONAL_ID_CONFIG);
        if (StringUtils.hasText(txId)) {
            setTransactionIdPrefix(txId);
        }
    }

    /**
     * Close the {@link Producer}(s) and clear the cache of transactional
     * {@link Producer}(s).
     */
    @Override
    public void reset() {
        try {
            destroy();
        } catch (Exception e) {
            LOGGER.error("Exception while closing producer", e);
        }
    }

    @Override
    public Producer<String, V> createProducer() {
        return createProducer(this.transactionIdPrefix);
    }

    @Override
    public Producer<String, V> createProducer(@Nullable String txIdPrefixArg) {
        String txIdPrefix = txIdPrefixArg == null ? this.transactionIdPrefix : txIdPrefixArg;
        return doCreateProducer(txIdPrefix);
    }

    @Override
    public boolean transactionCapable() {
        return this.transactionIdPrefix != null;
    }

    private Producer<String, V> doCreateProducer(String txIdPrefix) {
        this.globalLock.lock();
        try {
            return createTransactionalProducer(txIdPrefix);
        } finally {
            this.globalLock.unlock();
        }
    }

    protected Producer<String, V> createTransactionalProducer(String txIdPrefix) {
        BlockingQueue<CloseSafeProducer<String, V>> queue = getCache(txIdPrefix);
        Assert.notNull(queue, () -> "No cache found for " + txIdPrefix);
        CloseSafeProducer<String, V> cachedProducer = queue.poll();
        while (cachedProducer != null) {
            if (expire(cachedProducer)) {
                cachedProducer = queue.poll();
            } else {
                break;
            }
        }
        if (cachedProducer == null) {
            String suffix = this.transactionIdSuffixStrategy.acquireSuffix(txIdPrefix);
            return doCreateTxProducer(txIdPrefix, suffix, this::cacheReturner);
        } else {
            return cachedProducer;
        }
    }

    @Nullable
    protected BlockingQueue<CloseSafeProducer<String, V>> getCache(@Nullable String txIdPrefix) {
        if (txIdPrefix == null) {
            return null;
        }
        return this.cache.computeIfAbsent(txIdPrefix, txId -> new LinkedBlockingQueue<>());
    }

    private CloseSafeProducer<String, V> doCreateTxProducer(String prefix, String suffix,
                                                            BiPredicate<CloseSafeProducer<String, V>, Duration> remover) {


        Producer<String, V> newProducer = createRawProducer(prefix + suffix);
        try {
            newProducer.initTransactions();
        } catch (RuntimeException initTransactionException) {
            try {
                newProducer.close(this.physicalCloseTimeout);
            } catch (RuntimeException closeProducerException) {
                KafkaException combinedException = new KafkaException("initTransactions() failed and then close() failed", initTransactionException);
                combinedException.addSuppressed(closeProducerException);
                throw combinedException; // NOSONAR - lost stack trace
            } finally {
                this.transactionIdSuffixStrategy.releaseSuffix(prefix, suffix);
            }
            throw new KafkaException("initTransactions() failed", initTransactionException);
        }
        return new CloseSafeProducer<>(newProducer, remover, prefix, suffix, this.physicalCloseTimeout,
                this.epoch.get());
    }

    boolean cacheReturner(CloseSafeProducer<String, V> producerToRemove, Duration timeout) {
        if (producerToRemove.isClosed()) {
            this.removeTransactionProducer(producerToRemove);
            return true;
        }

        this.globalLock.lock();
        try {
            if (producerToRemove.getEpoch() != this.epoch.get()) {
                this.removeTransactionProducer(producerToRemove);
                return true;
            }
            BlockingQueue<CloseSafeProducer<String, V>> txIdCache = getCache(producerToRemove.getTxIdPrefix());
            if (producerToRemove.getEpoch() != this.epoch.get()
                || (txIdCache != null && !txIdCache.contains(producerToRemove)
                    && !txIdCache.offer(producerToRemove))) {
                this.removeTransactionProducer(producerToRemove);
                return true;
            }
        } finally {
            this.globalLock.unlock();
        }
        return false;
    }

    private void removeTransactionProducer(CloseSafeProducer<String, V> producer) {
        this.transactionIdSuffixStrategy.releaseSuffix(producer.getTxIdPrefix(), producer.getTxIdSuffix());
    }

    protected Producer<String, V> createRawProducer(String txId) {
        properties.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, txId);
        return new KafkaProducer<>(this.properties, getKeySerializer(), getValueSerializer());
    }

    @Override
    public Serializer<V> getValueSerializer() {
        return valueSerializer;
    }

    @Override
    public Serializer<String> getKeySerializer() {
        return keySerializer;
    }

    @Override
    public void destroy() {
        this.cache.values().forEach(queue -> {
            CloseSafeProducer<String, V> next = queue.poll();
            while (next != null) {
                try {
                    next.closeDelegate(this.physicalCloseTimeout);
                } catch (Exception e) {
                    LOGGER.error("Exception while closing producer", e);
                }
                next = queue.poll();
            }
        });
        this.cache.clear();
        this.epoch.incrementAndGet();
    }

    /**
     * Set a prefix for the {@link ProducerConfig#TRANSACTIONAL_ID_CONFIG} config. By
     * default, a {@link ProducerConfig#TRANSACTIONAL_ID_CONFIG} value from configs is used
     * as a prefix in the target producer configs.
     *
     * @param transactionIdPrefix the prefix.
     */
    public final void setTransactionIdPrefix(String transactionIdPrefix) {
        Assert.notNull(transactionIdPrefix, "'transactionIdPrefix' cannot be null");
        this.transactionIdPrefix = transactionIdPrefix;
        enableIdempotentBehaviour();
    }

    /**
     * The time to wait when physically closing the producer via the factory rather than
     * closing the producer itself (when {@link #reset()}, {@link #destroy()}).
     * Specified in seconds; default {@link #DEFAULT_PHYSICAL_CLOSE_TIMEOUT}.
     *
     * @param physicalCloseTimeout the timeout in seconds.
     */
    public void setPhysicalCloseTimeout(int physicalCloseTimeout) {
        this.physicalCloseTimeout = Duration.ofSeconds(physicalCloseTimeout);
    }

    /**
     * Set the maximum age for a producer; useful when using transactions and the broker
     * might expire a {@code transactional.id} due to inactivity.
     *
     * @param maxAge the maxAge to set
     */
    public void setMaxAge(Duration maxAge) {
        this.maxAge = maxAge.toMillis();
    }

    /**
     * Set the transaction suffix strategy.
     *
     * @param transactionIdSuffixStrategy the strategy.
     */
    public void setTransactionIdSuffixStrategy(TransactionIdSuffixStrategy transactionIdSuffixStrategy) {
        Assert.notNull(transactionIdSuffixStrategy, "'transactionIdSuffixStrategy' cannot be null");
        this.transactionIdSuffixStrategy = transactionIdSuffixStrategy;
    }

    /**
     * When set to 'true', the producer will ensure that exactly one copy of each message is written in the stream.
     */
    private void enableIdempotentBehaviour() {
        Object previousValue = this.properties.putIfAbsent(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        if (Boolean.FALSE.equals(previousValue)) {
            LOGGER.debug("The '" + ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG
                         + "' is set to false, may result in duplicate messages");
        }
    }

    private boolean expire(CloseSafeProducer<String, V> producer) {
        boolean expired = this.maxAge > 0 && System.currentTimeMillis() - producer.getCreated() > this.maxAge;
        if (expired) {
            producer.closeDelegate(this.physicalCloseTimeout);
        }
        return expired;
    }
}
