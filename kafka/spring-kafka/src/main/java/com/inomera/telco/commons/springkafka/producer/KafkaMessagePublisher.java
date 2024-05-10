package com.inomera.telco.commons.springkafka.producer;

import com.inomera.telco.commons.springkafka.PartitionKeyAware;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SerializationUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author Ramazan Karakaya
 */
public class KafkaMessagePublisher<V> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMessagePublisher.class);

    private Producer<String, V> producer;
    private boolean transactional;

    public KafkaMessagePublisher(Serializer<V> valueSerializer, Properties properties) {
        this.transactional = StringUtils.isNotBlank(properties.getProperty(ProducerConfig.TRANSACTIONAL_ID_CONFIG, StringUtils.EMPTY));
        this.producer = new KafkaProducer<>(properties, new StringSerializer(), valueSerializer);
        checkTransactionIfAvailableInit();
    }

    public KafkaMessagePublisher(Serializer<V> valueSerializer, Properties properties, boolean transactional) {
        this.transactional = transactional;
        this.producer = new KafkaProducer<>(properties, new StringSerializer(), valueSerializer);
        checkTransactionIfAvailableInit();
    }

    public Future<SendResult<String, V>> send(String topicName, V data) {
        final String partitionKey = getPartitionKey(data);
        final ProducerRecord<String, V> producerRecord = new ProducerRecord<>(topicName, partitionKey, data);
        return this.transactional ? doSendWithTransaction(producerRecord) : doSend(producerRecord);
    }

    public Future<SendResult<String, V>> send(String topicName, Map<String, Object> headers, V data) {
        final String partitionKey = getPartitionKey(data);
        final ProducerRecord<String, V> producerRecord = new ProducerRecord<>(topicName, partitionKey, data);
        checkAndSetHeaders(headers, producerRecord);
        return this.transactional ? doSendWithTransaction(producerRecord) : doSend(producerRecord);
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

    public Future<SendResult<String, V>> send(String topicName, String key, V data) {
        final ProducerRecord<String, V> producerRecord = new ProducerRecord<>(topicName, key, data);
        return this.transactional ? doSendWithTransaction(producerRecord) : doSend(producerRecord);
    }

    private String getPartitionKey(V data) {
        if (data instanceof PartitionKeyAware) {
            return ((PartitionKeyAware) data).getPartitionKey();
        }
        return String.valueOf(data.hashCode());
    }

    public Map<MetricName, ? extends Metric> metrics() {
        return producer.metrics();
    }

    public void flushAndClose() {
        producer.flush();
        close();
    }

    protected void close() {
        producer.close();
    }

    private Future<SendResult<String, V>> doSendWithTransaction(final ProducerRecord<String, V> producerRecord) {
        LOGGER.trace("Sending: {}", producerRecord);

        final CompletableFuture<SendResult<String, V>> future = new CompletableFuture<>();

        final Callback kafkaProducerSendCallback = (RecordMetadata metadata, Exception exception) -> {
            try {
                if (exception == null) {
                    future.complete(new SendResult<>(producerRecord, metadata));
                    LOGGER.trace("ProducerRecord: {}, Record Metadata: {}", producerRecord, metadata);
                } else {
                    LOGGER.error("Error publishing request. {}", producerRecord, exception);
                    future.completeExceptionally(exception);
                }
            } finally {
                closeProducerIfAvailable();
            }
        };

        try {
            LOGGER.debug("beginTransaction()");
            try {
                this.producer.beginTransaction();
            } catch (RuntimeException e) {
                LOGGER.error("beginTransaction failed: ", e);
                throw e;
            } finally {
                closeProducerIfAvailable();
            }
            producer.send(producerRecord, kafkaProducerSendCallback);
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
            closeProducerIfAvailable();
        }

        LOGGER.trace("Sent: {}", producerRecord);

        return future;
    }

    private Future<SendResult<String, V>> doSend(final ProducerRecord<String, V> producerRecord) {
        LOGGER.trace("Sending: {}", producerRecord);

        final CompletableFuture<SendResult<String, V>> future = new CompletableFuture<>();

        final Callback kafkaProducerSendCallback = (RecordMetadata metadata, Exception exception) -> {
            LOGGER.trace("ProducerRecord: {}, Record Metadata: {}, Exception: {}", producerRecord, metadata, exception.getMessage(), exception);
            LOGGER.error("Error publishing request. {}", producerRecord, exception);
            future.completeExceptionally(exception);
        };

        try {
            producer.send(producerRecord, kafkaProducerSendCallback);
        } catch (InterruptException e) {
            LOGGER.info("Producer is interrupted and not able to send message {}", producerRecord);
            future.completeExceptionally(e);
        } catch (Exception e) {
            LOGGER.error("Exception publishing request. {}", producerRecord, e);
            future.completeExceptionally(e);
        }

        LOGGER.trace("Sent: {}", producerRecord);

        return future;
    }

    private void closeProducerIfAvailable() {
        if (this.producer == null || this.transactional) {
            return;
        }
        this.producer.close(Duration.ofMillis(5000L));
    }

    private void checkTransactionIfAvailableInit() {
        if (!this.transactional) {
            return;
        }
        try {
            this.producer.initTransactions();
        } catch (RuntimeException ex) {
            try {
                this.producer.close(Duration.ofMillis(30000L));
            } catch (RuntimeException re) {
                KafkaException ke = new KafkaException("initTransactions() failed and then close() failed", ex);
                ke.addSuppressed(re);
                throw ke;
            }
            throw new KafkaException("initTransactions() failed", ex);
        }
    }

    public static class SendResult<K, V> {
        private final ProducerRecord<K, V> producerRecord;
        private final RecordMetadata recordMetadata;

        public SendResult(ProducerRecord<K, V> producerRecord, RecordMetadata recordMetadata) {
            this.producerRecord = producerRecord;
            this.recordMetadata = recordMetadata;
        }

        public ProducerRecord<K, V> getProducerRecord() {
            return this.producerRecord;
        }

        public RecordMetadata getRecordMetadata() {
            return this.recordMetadata;
        }
    }
}
