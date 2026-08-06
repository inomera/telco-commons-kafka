package com.inomera.telco.commons.springkafka.e2e;

import com.inomera.telco.commons.springkafka.consumer.invoker.ListenerMethod;
import com.inomera.telco.commons.springkafka.consumer.invoker.ListenerMethodRegistry;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Shared plumbing for the end-to-end tests.
 *
 * <p>These tests need the 3-broker KRaft cluster from
 * {@code example/spring-kafka-protobuf/src/main/resources/docker-compose.yml}:
 *
 * <pre>
 * cd example/spring-kafka-protobuf/src/main/resources && docker compose up -d
 * </pre>
 *
 * When no broker answers on the external listeners the tests skip themselves, so
 * {@code ./gradlew build} still passes on a machine (or CI runner) without Docker.
 */
final class E2eSupport {
    private static final Logger LOG = LoggerFactory.getLogger(E2eSupport.class);

    /** External listeners advertised by the compose file. */
    static final String BOOTSTRAP = "127.0.0.1:10000,127.0.0.1:20000,127.0.0.1:30000";

    private E2eSupport() {
    }

    // ------------------------------------------------------------------ gate

    static boolean brokerAvailable() {
        for (String hostPort : BOOTSTRAP.split(",")) {
            final String[] parts = hostPort.split(":");
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])), 1_500);
                return true;
            } catch (Exception e) {
                LOG.debug("Broker {} not reachable: {}", hostPort, e.getMessage());
            }
        }
        return false;
    }

    // ----------------------------------------------------------------- admin

    static void createTopic(String topic, int partitions) {
        createTopic(topic, partitions, (short) 1);
    }

    /**
     * Creates a topic with an explicit replication factor.
     *
     * <p>The default of 1 is fine for tests that only need a place to put records, but it makes
     * a topic unsurvivable: stopping a broker takes every partition it leads offline, so the
     * producer cannot write to them and the consumer cannot read them. Any test that stops a
     * broker and still expects traffic to flow has to replicate.
     */
    static void createTopic(String topic, int partitions, short replicationFactor) {
        final Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP);
        try (Admin admin = Admin.create(props)) {
            admin.createTopics(List.of(new NewTopic(topic, partitions, replicationFactor))).all().get();
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw new IllegalStateException("Could not create topic " + topic, e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    static void deleteTopic(String topic) {
        final Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP);
        try (Admin admin = Admin.create(props)) {
            admin.deleteTopics(List.of(topic)).all().get();
        } catch (Exception e) {
            LOG.debug("Could not delete topic {}: {}", topic, e.getMessage());
        }
    }

    // -------------------------------------------------------------- producer

    /** Publishes {@code count} records keyed {@code key-<i>} with payload {@code payloadPrefix<i>}. */
    static void publish(String topic, String payloadPrefix, int count) {
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.LINGER_MS_CONFIG, "5");
        try (Producer<String, String> producer =
                     new KafkaProducer<>(props, new StringSerializer(), new StringSerializer())) {
            for (int i = 0; i < count; i++) {
                producer.send(new ProducerRecord<>(topic, "key-" + i, payloadPrefix + i));
            }
            producer.flush();
        }
    }

    static void publishOne(String topic, String key, String payload) {
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        try (Producer<String, String> producer =
                     new KafkaProducer<>(props, new StringSerializer(), new StringSerializer())) {
            producer.send(new ProducerRecord<>(topic, key, payload));
            producer.flush();
        }
    }

    // -------------------------------------------------------------- consumer

    /** Consumer properties tuned for fast, deterministic tests. */
    static Properties consumerProperties() {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");
        props.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, "2000");
        return props;
    }

    /** Deserializer that hands the listener a {@link String}. */
    static final class StringPayloadDeserializer implements Deserializer<Object> {
        @Override
        public Object deserialize(String topic, byte[] data) {
            return data == null ? null : new String(data, StandardCharsets.UTF_8);
        }
    }

    /**
     * Deserializer that returns {@code null} for poisoned payloads, mirroring what the real
     * serializer modules do when they cannot decode a record.
     */
    static final class PoisonAwareDeserializer implements Deserializer<Object> {
        private final String poisonMarker;

        PoisonAwareDeserializer(String poisonMarker) {
            this.poisonMarker = poisonMarker;
        }

        @Override
        public Object deserialize(String topic, byte[] data) {
            if (data == null) {
                return null;
            }
            final String value = new String(data, StandardCharsets.UTF_8);
            // Throwing here would kill the consumer thread, so the contract is to return null.
            return value.contains(poisonMarker) ? null : value;
        }
    }

    // ------------------------------------------------------------- listeners

    /**
     * Registers {@code methodName} on {@code bean} as a listener, bypassing the Spring
     * {@code BeanPostProcessor} so the tests need no application context.
     */
    static void registerListener(ListenerMethodRegistry registry, String groupId, String topic,
                                 Class<?> messageType, Object bean, String methodName) {
        try {
            final Method method = bean.getClass().getDeclaredMethod(methodName, messageType);
            method.setAccessible(true);
            registry.addListenerMethod(groupId, topic, messageType, new ListenerMethod(bean, method));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("No listener method " + methodName, e);
        }
    }

    // ----------------------------------------------------------- group state

    /** Committed offset for a partition, or {@code -1} when the group has never committed. */
    static long committedOffset(String groupId, TopicPartition partition) {
        final Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP);
        try (Admin admin = Admin.create(props)) {
            final OffsetAndMetadata offset = admin.listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata().get().get(partition);
            return offset == null ? -1L : offset.offset();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read committed offsets for " + groupId, e);
        }
    }

    /** Log-end offset, i.e. the offset the next produced record will get. */
    static long endOffset(TopicPartition partition) {
        final Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP);
        try (Admin admin = Admin.create(props)) {
            return admin.listOffsets(Map.of(partition, OffsetSpec.latest()))
                    .partitionResult(partition).get().offset();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read end offset for " + partition, e);
        }
    }

    // ----------------------------------------------------------- chaos hooks

    /** Container names from the compose file, in the order they are declared. */
    static final List<String> BROKER_CONTAINERS = List.of("broker", "broker-2", "broker-3");
    static final List<Integer> BROKER_PORTS = List.of(10000, 20000, 30000);

    static void dockerStop(String container) {
        docker("stop", container);
    }

    static void dockerStart(String container) {
        docker("start", container);
    }

    private static void docker(String command, String container) {
        try {
            final Process process = new ProcessBuilder("docker", command, container)
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(90, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("docker " + command + " " + container + " timed out");
            }
            final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new IllegalStateException("docker " + command + " " + container + " failed: " + output);
            }
            LOG.info("docker {} {} -> {}", command, container, output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not run docker " + command, e);
        }
    }

    static boolean portOpen(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static void awaitPort(int port, boolean expectedOpen, Duration timeout) {
        await("port " + port + (expectedOpen ? " to open" : " to close"), timeout,
                () -> portOpen(port) == expectedOpen);
    }

    /** Restores every broker and waits for its external listener, whatever a test did to them. */
    static void restoreCluster() {
        for (String container : BROKER_CONTAINERS) {
            try {
                dockerStart(container);
            } catch (Exception e) {
                LOG.warn("Could not restart {}: {}", container, e.getMessage());
            }
        }
        for (int port : BROKER_PORTS) {
            try {
                awaitPort(port, true, Duration.ofSeconds(90));
            } catch (AssertionError e) {
                LOG.warn("Broker port {} did not come back: {}", port, e.getMessage());
            }
        }
    }

    // ----------------------------------------------------------------- await

    static void await(String what, Duration timeout, BooleanSupplier condition) {
        final Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }
            sleep(25);
        }
        throw new AssertionError("Timed out after " + timeout + " waiting for " + what);
    }

    /** Gives a condition a chance to be violated; used to assert that nothing extra arrives. */
    static void settle(Duration duration) {
        sleep(duration.toMillis());
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted", e);
        }
    }
}
