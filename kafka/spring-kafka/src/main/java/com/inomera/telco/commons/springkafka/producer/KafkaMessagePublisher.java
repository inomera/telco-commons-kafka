package com.inomera.telco.commons.springkafka.producer;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public KafkaMessagePublisher(Serializer<V> valueSerializer, Properties properties) {
        this.producer = new KafkaProducer<>(properties, new StringSerializer(), valueSerializer);
    }

    public Future<SendResult<String, V>> send(String topicName, V data) {
        final String partitionKey = getPartitionKey(data);
        final ProducerRecord<String, V> producerRecord = new ProducerRecord<>(topicName, partitionKey, data);
        return doSend(producerRecord);
    }

    public Future<SendResult<String, V>> send(String topicName, String key, V data) {
        final ProducerRecord<String, V> producerRecord = new ProducerRecord<>(topicName, key, data);
        return doSend(producerRecord);
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

    private Future<SendResult<String, V>> doSend(final ProducerRecord<String, V> producerRecord) {
        LOGGER.trace("Sending: {}", producerRecord);

        final CompletableFuture<SendResult<String, V>> future = new CompletableFuture<>();

        final Callback kafkaProducerSendCallback = (RecordMetadata metadata, Exception exception) -> {
            LOGGER.trace("ProducerRecord: {}, Record Metadata: {}, Exception: {}", producerRecord, metadata, exception);

            if (exception == null) {
                future.complete(new SendResult<>(producerRecord, metadata));
            } else {
                LOGGER.error("Error publishing request. {}", producerRecord, exception);
                future.completeExceptionally(exception);
            }
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
