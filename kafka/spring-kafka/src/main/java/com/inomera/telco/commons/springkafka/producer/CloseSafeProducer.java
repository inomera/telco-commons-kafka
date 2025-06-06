package com.inomera.telco.commons.springkafka.producer;

import lombok.Getter;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.*;
import org.apache.kafka.common.errors.OutOfOrderSequenceException;
import org.apache.kafka.common.errors.ProducerFencedException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.telemetry.internals.ClientTelemetryReporter;
import org.apache.kafka.common.telemetry.internals.ClientTelemetryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.BiPredicate;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

@Getter
public class CloseSafeProducer<K, V> implements Producer<K, V> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloseSafeProducer.class);
    private static final Duration CLOSE_TIMEOUT_AFTER_TX_TIMEOUT = Duration.ofMillis(0);

    private final Producer<K, V> delegate;
    private final BiPredicate<CloseSafeProducer<K, V>, Duration> removeProducer;
    private final String txIdPrefix;
    private final String txIdSuffix;
    private final long created;
    private final Duration closeTimeout;
    private final int epoch;
    private final Optional<ClientTelemetryReporter> clientTelemetryReporter;

    private volatile Exception producerFailed;
    private volatile boolean closed;


    CloseSafeProducer(Producer<K, V> delegate,
                      BiPredicate<CloseSafeProducer<K, V>, Duration> removeConsumerProducer, Duration closeTimeout,
                      int epoch) {
        this(delegate, removeConsumerProducer, null, null, closeTimeout, epoch);
    }

    CloseSafeProducer(Producer<K, V> delegate, BiPredicate<CloseSafeProducer<K, V>, Duration> removeProducer,
                      @Nullable String txIdPrefix, ProducerConfig producerConfig, Duration closeTimeout, int epoch) {
        this(delegate, removeProducer, txIdPrefix, null, producerConfig, closeTimeout, epoch);
    }

    CloseSafeProducer(Producer<K, V> delegate,
                      BiPredicate<CloseSafeProducer<K, V>, Duration> removeProducer, @Nullable String txIdPrefix,
                      @Nullable String txIdSuffix, ProducerConfig producerConfig, Duration closeTimeout, int epoch) {
        Assert.isTrue(!(delegate instanceof CloseSafeProducer), "Cannot double-wrap a producer");
        this.delegate = delegate;
        this.removeProducer = removeProducer;
        this.txIdPrefix = txIdPrefix;
        this.txIdSuffix = txIdSuffix;
        this.closeTimeout = closeTimeout;
        this.created = System.currentTimeMillis();
        this.epoch = epoch;
        this.clientTelemetryReporter = CommonClientConfigs.telemetryReporter(txIdPrefix, producerConfig);
        LOGGER.debug("Created new Producer: {}", this);
    }


    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record) {
        LOGGER.trace("{} send({})", this, record);
        return this.delegate.send(record);
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback) {
        LOGGER.trace("{} send({})", this, record);

        return this.delegate.send(record, (metadata, exception) -> {
            if (exception instanceof OutOfOrderSequenceException) {
                CloseSafeProducer.this.producerFailed = exception;
                close(CloseSafeProducer.this.closeTimeout);
            }
            callback.onCompletion(metadata, exception);
        });
    }

    @Override
    public void flush() {
        LOGGER.trace("{} flush()", this);
        this.delegate.flush();
    }

    @Override
    public List<PartitionInfo> partitionsFor(String topic) {
        return this.delegate.partitionsFor(topic);
    }

    @Override
    public Map<MetricName, ? extends Metric> metrics() {
        return this.delegate.metrics();
    }

    @Override
    public Uuid clientInstanceId(Duration timeout) {
        if (clientTelemetryReporter.isEmpty()) {
            throw new IllegalStateException("Telemetry is not enabled. Set config `" + ProducerConfig.ENABLE_METRICS_PUSH_CONFIG + "` to `true`.");
        }

        return ClientTelemetryUtils.fetchClientInstanceId(clientTelemetryReporter.get(), timeout);
    }

    @Override
    public void initTransactions() {
        this.delegate.initTransactions();
    }

    @Override
    public void beginTransaction() throws ProducerFencedException {
        LOGGER.debug("{} beginTransaction()", this);
        try {
            this.delegate.beginTransaction();
        } catch (RuntimeException e) {
            LOGGER.error("beginTransaction failed: {}", this, e);
            this.producerFailed = e;
            throw e;
        }
    }

    @Override
    public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets,
                                         ConsumerGroupMetadata groupMetadata) throws ProducerFencedException {
        LOGGER.trace("{} sendOffsetsToTransaction({}, {})", this, offsets, groupMetadata);
        this.delegate.sendOffsetsToTransaction(offsets, groupMetadata);
    }

    @Override
    public void commitTransaction() throws ProducerFencedException {
        LOGGER.debug("{} commitTransaction()", this);
        try {
            this.delegate.commitTransaction();
        } catch (RuntimeException e) {
            LOGGER.error("commitTransaction failed: {}", this, e);
            this.producerFailed = e;
            throw e;
        }
    }

    @Override
    public void abortTransaction() throws ProducerFencedException {
        LOGGER.debug("{} abortTransaction()", this);
        if (this.producerFailed != null) {
            LOGGER.debug("abortTransaction ignored - previous txFailed: {}: {}", this.producerFailed.getMessage(), this);
            return;
        }
        try {
            this.delegate.abortTransaction();
        } catch (RuntimeException e) {
            LOGGER.error("Abort failed: {}", this, e);
            this.producerFailed = e;
            throw e;
        }
    }

    @Override
    public void registerMetricForSubscription(KafkaMetric metric) {
        LOGGER.info("delegate registerMetricForSubscription: {}", metric);
        this.delegate.registerMetricForSubscription(metric);
    }

    @Override
    public void unregisterMetricFromSubscription(KafkaMetric metric) {
        LOGGER.info("delegate unregisterMetricFromSubscription: {}", metric);
        this.delegate.unregisterMetricFromSubscription(metric);
    }

    @Override
    public void close() {
        close(null);
    }

    @Override
    public void close(@Nullable Duration timeout) {
        LOGGER.trace("{} close({})", this, timeout);
        if (this.closed) {
            return;
        }

        if (this.producerFailed != null) {
            LOGGER.warn("Error during some operation; producer removed from cache: {}", this);
            this.closed = true;
            this.removeProducer.test(this, getCloseTimeout(timeout));
            this.delegate.close(timeout == null ? this.closeTimeout : getCloseTimeout(timeout));
            return;
        }

        this.closed = this.removeProducer.test(this, timeout);
        if (this.closed) {
            this.delegate.close(timeout == null ? this.closeTimeout : timeout);
        }
    }

    void closeDelegate(Duration timeout) {
        try {
            if (!this.closed) {
                this.delegate.close(defaultIfNull(timeout, this.closeTimeout));
                this.closed = true;
                this.removeProducer.test(this, defaultIfNull(timeout, this.closeTimeout));
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to close {}", this.delegate, ex);
        }
    }

    @Override
    public String toString() {
        return "CloseSafeProducer [delegate=" + this.delegate + "]";
    }

    private Duration getCloseTimeout(Duration timeout) {
        return this.producerFailed instanceof TimeoutException
                ? CLOSE_TIMEOUT_AFTER_TX_TIMEOUT
                : timeout;
    }

}
