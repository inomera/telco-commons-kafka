package com.inomera.telco.commons.example.springkafka;

import com.google.protobuf.GeneratedMessage;
import com.inomera.telco.commons.example.domain.constant.DomainConstants;
import com.inomera.telco.commons.example.domain.constant.KafkaTopicUtils;
import com.inomera.telco.commons.kafkaprotobuf.ImmutableClassIdRegistry;
import com.inomera.telco.commons.kafkaprotobuf.KafkaProtobufSerializer;
import messaging.OrderMessage;
import messaging.PartitionMessage;
import messaging.PaymentMessage;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import player.command.PlayerCreateCommandProto;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Publishes real protobuf traffic into the topics the running example app listens on, so its
 * consumers can be exercised live.
 *
 * <p>The example's own {@code SomeSender.publishRandomText()} short-circuits with
 * {@code if (running.get() || true) return;}, so hitting {@code /start-produce} publishes
 * nothing. This driver fills that gap without touching the example's business logic.
 *
 * <p>Requires the broker from {@code src/main/resources/docker-compose.yml}; skips otherwise.
 * It only produces — the consumers live in the separately running application.
 */
class LiveTrafficDriverTest {
    private static final Logger LOG = LoggerFactory.getLogger(LiveTrafficDriverTest.class);

    private static final String BOOTSTRAP = "127.0.0.1:10000,127.0.0.1:20000,127.0.0.1:30000";
    private static final int COUNT = 25;

    private static boolean brokerAvailable() {
        for (String hostPort : BOOTSTRAP.split(",")) {
            final String[] parts = hostPort.split(":");
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])), 1_500);
                return true;
            } catch (Exception ignored) {
                // try the next broker
            }
        }
        return false;
    }

    private static Producer<String, GeneratedMessage> producer() {
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.LINGER_MS_CONFIG, "5");
        // Same class-id registry the application uses, so its deserializer can decode these.
        return new KafkaProducer<>(props, new StringSerializer(),
                new KafkaProtobufSerializer(new ImmutableClassIdRegistry(DomainConstants.CLASS_IDS)));
    }

    @Test
    @DisplayName("drive live protobuf traffic through both the OS-thread and virtual-thread consumers")
    void driveLiveTraffic() {
        assumeTrue(brokerAvailable(), "No Kafka broker on " + BOOTSTRAP + "; start docker compose first");

        try (Producer<String, GeneratedMessage> producer = producer()) {
            // event-logger (OS threads): single-record + bulk listeners on this topic.
            final String playerTopic = KafkaTopicUtils.getTopicName(PlayerCreateCommandProto.class);
            for (int i = 0; i < COUNT; i++) {
                final PlayerCreateCommandProto message = PlayerCreateCommandProto.newBuilder()
                        .setTxKey("live-player-" + i)
                        .build();
                producer.send(new ProducerRecord<>(playerTopic, "player-" + i, message));
            }

            // virtual-event-logger (virtual threads): single-record listener.
            final String orderTopic = KafkaTopicUtils.getTopicName(OrderMessage.class);
            for (int i = 0; i < COUNT; i++) {
                final OrderMessage message = OrderMessage.newBuilder()
                        .setPartition(PartitionMessage.newBuilder()
                                .setPartitionKey("order-key-" + i)
                                .setLogTrackKey("live-order-" + i)
                                .build())
                        .setOrderId("order-" + i)
                        .setCustomerName("customer-" + i)
                        .build();
                producer.send(new ProducerRecord<>(orderTopic, "order-" + i, message));
            }

            // virtual-event-logger (virtual threads): bulk listener.
            final String paymentTopic = KafkaTopicUtils.getTopicName(PaymentMessage.class);
            for (int i = 0; i < COUNT; i++) {
                final PaymentMessage message = PaymentMessage.newBuilder()
                        .setPartition(PartitionMessage.newBuilder()
                                .setPartitionKey("payment-key-" + i)
                                .setLogTrackKey("live-payment-" + i)
                                .build())
                        .setPaymentId("payment-" + i)
                        .setAmount(10.5d * (i + 1))
                        .build();
                producer.send(new ProducerRecord<>(paymentTopic, "payment-" + i, message));
            }

            producer.flush();
            LOG.info("Published {} records to each of {}, {}, {}", COUNT, playerTopic, orderTopic, paymentTopic);
        }
    }
}
