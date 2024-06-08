package com.inomera.telco.commons.springkafka.producer;

import com.inomera.telco.commons.springkafka.PartitionKeyAware;
import lombok.Getter;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class KafkaTransactionalMessagePublisher<V> implements DisposableBean {
    private static final String NON_TRANSACTIONAL_PRODUCERS_ARE_NOT_SUPPORTED = "Non transactional producers are not supported";
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaTransactionalMessagePublisher.class);

    private final ProducerFactory<V> producerFactory;
    private final Map<Thread, Producer<String, V>> producers = new ConcurrentHashMap<>();

    private Duration closeTimeout = Duration.ofSeconds(5);

    public KafkaTransactionalMessagePublisher(Serializer<V> valueSerializer, Properties properties) {
        Assert.isTrue(isTransactional(properties), NON_TRANSACTIONAL_PRODUCERS_ARE_NOT_SUPPORTED);
        this.producerFactory = new DefaultKafkaProducerFactory<>(valueSerializer, properties);
    }

    public Future<SendResult<String, V>> send(String topicName, V data) {
        final String partitionKey = getPartitionKey(data);
        final ProducerRecord<String, V> producerRecord = new ProducerRecord<>(topicName, partitionKey, data);
        return doSendWithTransaction(producerRecord);
    }

    public Future<SendResult<String, V>> send(String topicName, Map<String, Object> headers, V data) {
        final String partitionKey = getPartitionKey(data);
        final ProducerRecord<String, V> producerRecord = new ProducerRecord<>(topicName, partitionKey, data);
        checkAndSetHeaders(headers, producerRecord);
        return doSendWithTransaction(producerRecord);
    }

    public Future<SendResult<String, V>> send(String topicName, String key, V data) {
        final ProducerRecord<String, V> producerRecord = new ProducerRecord<>(topicName, key, data);
        return doSendWithTransaction(producerRecord);
    }

    @Override
    public void destroy() {
        ((DefaultKafkaProducerFactory<V>) this.producerFactory).destroy();
    }

    private Future<SendResult<String, V>> doSendWithTransaction(final ProducerRecord<String, V> producerRecord) {
        Thread currentThread = Thread.currentThread();
        Producer<String, V> producer = this.producers.get(currentThread);
        Assert.state(producer == null, "Nested calls to 'doSendWithTransaction' are not allowed");
        producer = this.producerFactory.createProducer();

        LOGGER.debug("beginTransaction()");
        try {
            producer.beginTransaction();
        } catch (Exception e) {
            LOGGER.error("beginTransaction failed: ", e);
            producer.close(this.closeTimeout);
            throw e;
        }
        this.producers.put(currentThread, producer);

        LOGGER.trace("Sending: {}", producerRecord);
        final CompletableFuture<SendResult<String, V>> future = new CompletableFuture<>();
        try {
            Callback callback = buildCallback(future, producerRecord);
            producer.send(producerRecord, callback);
            try {
                producer.commitTransaction();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (InterruptException e) {
            LOGGER.info("Producer is interrupted and not able to send message {}", producerRecord);
            future.completeExceptionally(e);
        } catch (Exception e) {
            LOGGER.error("Exception publishing request. {}", producerRecord, e);
            future.completeExceptionally(e);
            try {
                producer.abortTransaction();
            } catch (Exception abortException) {
                e.addSuppressed(abortException);
            }
        } finally {
            this.producers.remove(currentThread);
            producer.close(this.closeTimeout);
        }

        LOGGER.trace("Sent: {}", producerRecord);
        return future;
    }

    @Getter
    public static class SendResult<K, V> {
        private final ProducerRecord<K, V> producerRecord;
        private final RecordMetadata recordMetadata;

        public SendResult(ProducerRecord<K, V> producerRecord, RecordMetadata recordMetadata) {
            this.producerRecord = producerRecord;
            this.recordMetadata = recordMetadata;
        }
    }

    private boolean isTransactional(Properties properties) {
        String transactionalId = (String) properties.get(ProducerConfig.TRANSACTIONAL_ID_CONFIG);
        return StringUtils.hasText(transactionalId);
    }

    /**
     * Set the maximum time to wait when closing a producer; default 5 seconds.
     *
     * @param closeTimeout the close timeout.
     */
    public void setCloseTimeout(Duration closeTimeout) {
        Assert.notNull(closeTimeout, "'closeTimeout' cannot be null");
        this.closeTimeout = closeTimeout;
    }

    private <V> Callback buildCallback(CompletableFuture<SendResult<String, V>> future,
                                       ProducerRecord<String, V> producerRecord) {
        return (RecordMetadata metadata, Exception exception) -> {
            if (exception == null) {
                future.complete(new SendResult<>(producerRecord, metadata));
                LOGGER.trace("ProducerRecord: {}, Record Metadata: {}", producerRecord, metadata);
            } else {
                LOGGER.error("Error publishing request. {}", producerRecord, exception);
                future.completeExceptionally(exception);
            }
        };
    }

    private String getPartitionKey(V data) {
        if (data instanceof PartitionKeyAware) {
            return ((PartitionKeyAware) data).getPartitionKey();
        }
        return String.valueOf(data.hashCode());
    }

    private void checkAndSetHeaders(Map<String, Object> headers, ProducerRecord<String, V> producerRecord) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        headers.forEach((key, value) -> {
            final byte[] serializedHeaderValue = SerializationUtils.serialize(value);
            producerRecord.headers().add(new RecordHeader(key, serializedHeaderValue));
        });
    }
}
