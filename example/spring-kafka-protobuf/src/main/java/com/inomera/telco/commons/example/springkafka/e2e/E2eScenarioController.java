package com.inomera.telco.commons.example.springkafka.e2e;

import com.google.protobuf.GeneratedMessage;
import com.inomera.telco.commons.example.domain.constant.KafkaTopicUtils;
import com.inomera.telco.commons.springkafka.consumer.KafkaMessageConsumer;
import com.inomera.telco.commons.springkafka.producer.KafkaMessagePublisher;
import lombok.RequiredArgsConstructor;
import messaging.OrderMessage;
import messaging.PartitionMessage;
import messaging.PaymentMessage;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import player.command.PlayerCreateCommandProto;
import todo.command.TodoUpdateCommandProto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Drives the library's edge cases over HTTP so they can be triggered from an IDE run and watched
 * in the application log. Everything here only produces traffic or toggles consumer lifecycle —
 * the consumers themselves are the ordinary application beans.
 *
 * <pre>
 * GET /e2e/status                     counters and observed retry backoff gaps
 * GET /e2e/burst?count=100            publish to the OS-thread and virtual-thread consumers
 * GET /e2e/retry                      publish to a listener that always throws
 * GET /e2e/poison                     publish a record no deserializer can decode
 * GET /e2e/restart-consumers          stop and start both consumers (rebalance)
 * GET /e2e/reset                      reset counters
 * </pre>
 */
@RestController
@RequestMapping("/e2e")
@RequiredArgsConstructor
public class E2eScenarioController {
    private static final Logger LOG = LoggerFactory.getLogger(E2eScenarioController.class);

    /** No class is registered under this id, so the deserializer cannot decode it. */
    private static final int UNREGISTERED_CLASS_ID = 999_999;

    private final KafkaMessagePublisher<? super GeneratedMessage> kafkaPublisher;
    private final E2eMetrics metrics;

    @Qualifier("consumer")
    private final KafkaMessageConsumer osThreadConsumer;

    @Qualifier("virtualConsumer")
    private final KafkaMessageConsumer virtualConsumer;

    @GetMapping("/status")
    public Map<String, Object> status() {
        final Map<String, Object> snapshot = metrics.snapshot();
        LOG.info("E2E status: {}", snapshot);
        return snapshot;
    }

    @GetMapping("/reset")
    public Map<String, Object> reset() {
        metrics.reset();
        LOG.info("E2E counters reset");
        return Map.of("reset", true);
    }

    /**
     * Publishes to the topics consumed by both stacks. Watch for "OS THREAD!!" and
     * "VIRTUAL THREAD!!" lines to see each path pick the traffic up.
     */
    @GetMapping("/burst")
    public Map<String, Object> burst(@RequestParam(defaultValue = "100") int count) {
        LOG.info("E2E burst: publishing {} records per topic", count);
        final long startedAt = System.nanoTime();

        for (int i = 0; i < count; i++) {
            kafkaPublisher.send(KafkaTopicUtils.getTopicName(PlayerCreateCommandProto.class),
                    PlayerCreateCommandProto.newBuilder().setTxKey("burst-player-" + i).build());

            kafkaPublisher.send(KafkaTopicUtils.getTopicName(OrderMessage.class),
                    OrderMessage.newBuilder()
                            .setPartition(partition("order", i))
                            .setOrderId("order-" + i)
                            .setCustomerName("customer-" + i)
                            .build());

            kafkaPublisher.send(KafkaTopicUtils.getTopicName(PaymentMessage.class),
                    PaymentMessage.newBuilder()
                            .setPartition(partition("payment", i))
                            .setPaymentId("payment-" + i)
                            .setAmount(10.5d * (i + 1))
                            .build());
        }

        final long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;
        metrics.addProduced(3L * count);

        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("recordsPerTopic", count);
        result.put("totalRecords", 3 * count);
        result.put("publishMillis", elapsedMillis);
        LOG.info("E2E burst done: {} records in {}ms", 3 * count, elapsedMillis);
        return result;
    }

    /**
     * Publishes to {@link E2eRetryListener}, whose listener always throws. The log should show
     * "E2E retry attempt" lines spaced roughly by the configured backoff, then
     * "Retries exhausted" from the dead-letter handler. Before 4.1.0 the attempts were
     * back-to-back and nothing was dead-lettered.
     */
    @GetMapping("/retry")
    public Map<String, Object> retry(@RequestParam(defaultValue = "1") int count) {
        LOG.info("E2E retry scenario: publishing {} record(s) to {}", count, E2eRetryListener.TOPIC);
        for (int i = 0; i < count; i++) {
            kafkaPublisher.send(E2eRetryListener.TOPIC,
                    TodoUpdateCommandProto.newBuilder()
                            .setTxKey("retry-" + System.currentTimeMillis() + "-" + i)
                            .setInfo("always fails")
                            .setStatus("PENDING")
                            .build());
        }
        metrics.addRetryTopicProduced(count);
        return Map.of("published", count, "topic", E2eRetryListener.TOPIC,
                "hint", "watch for 'E2E retry attempt' spacing then 'Retries exhausted'");
    }

    /**
     * Writes a record whose class id is not in the registry. The deserializer returns
     * {@code null} rather than throwing — a throwing deserializer would kill the consumer
     * thread — so the consumer must log it and carry on.
     */
    @GetMapping("/poison")
    public Map<String, Object> poison() {
        final String topic = KafkaTopicUtils.getTopicName(OrderMessage.class);
        final byte[] payload = "not-a-valid-protobuf-payload".getBytes(StandardCharsets.UTF_8);
        final byte[] record = ByteBuffer.allocate(Integer.BYTES + payload.length)
                .putInt(UNREGISTERED_CLASS_ID)
                .put(payload)
                .array();

        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        try (Producer<String, byte[]> rawProducer =
                     new KafkaProducer<>(props, new StringSerializer(), new ByteArraySerializer())) {
            rawProducer.send(new ProducerRecord<>(topic, "poison-key", record));
            rawProducer.flush();
        }

        metrics.addPoisonProduced(1);
        LOG.warn("E2E poison record published to {} with unregistered class id {}", topic, UNREGISTERED_CLASS_ID);

        // Immediately follow it with a good record: it must still be consumed.
        kafkaPublisher.send(topic, OrderMessage.newBuilder()
                .setPartition(partition("after-poison", 0))
                .setOrderId("after-poison")
                .setCustomerName("survivor")
                .build());
        metrics.addProduced(1);

        return Map.of("poisonTopic", topic, "unregisteredClassId", UNREGISTERED_CLASS_ID,
                "hint", "the 'after-poison' order must still reach the listener");
    }

    /**
     * Stops and restarts both consumers. Watch REVOKED / ASSIGNED to confirm the group
     * re-forms; before 4.1.0 an incremental assignment could strand partitions entirely.
     */
    @GetMapping("/restart-consumers")
    public Map<String, Object> restartConsumers() {
        LOG.info("E2E restarting consumers");
        osThreadConsumer.stop();
        virtualConsumer.stop();
        osThreadConsumer.start();
        virtualConsumer.start();
        metrics.recordConsumerRestart();
        LOG.info("E2E consumers restarted");
        return Map.of("restarted", true, "hint", "watch REVOKED-> then ASSIGNED-> for both groups");
    }

    private static PartitionMessage partition(String prefix, int index) {
        return PartitionMessage.newBuilder()
                .setPartitionKey(prefix + "-key-" + index)
                .setLogTrackKey(prefix + "-track-" + index)
                .build();
    }

    private static String bootstrapServers() {
        return System.getProperty("e2e.bootstrap.servers",
                "127.0.0.1:10000,127.0.0.1:20000,127.0.0.1:30000");
    }
}
