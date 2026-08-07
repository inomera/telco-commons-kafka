package com.inomera.telco.commons.springkafka.e2e;

import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import com.inomera.telco.commons.springkafka.builder.KafkaConsumerBuilder;
import com.inomera.telco.commons.springkafka.consumer.KafkaConsumerProperties;
import com.inomera.telco.commons.springkafka.consumer.KafkaMessageConsumer;
import com.inomera.telco.commons.springkafka.consumer.OffsetCommitStrategy;
import com.inomera.telco.commons.springkafka.consumer.PollerThreadState;
import com.inomera.telco.commons.springkafka.consumer.PollerThreadStateChecker;
import com.inomera.telco.commons.springkafka.consumer.PollerThreadStore;
import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;
import com.inomera.telco.commons.springkafka.consumer.invoker.ListenerMethodRegistry;
import com.inomera.telco.commons.springkafka.consumer.poller.DefaultConsumerPoller;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Failure-mode end-to-end tests: brokers dying under load, commits failing, rebalances landing at
 * the worst possible moment, and a poller thread that has to come back on its own.
 *
 * <p>Separate from {@link KafkaE2eTest} because these tests <em>stop and start broker
 * containers</em>. That makes them slow, order-sensitive against the shared cluster, and unfit to
 * run in parallel with anything else. {@link #tearDown()} puts the cluster back together after
 * each test so a failure here cannot cascade into the next one.
 *
 * <p>Requires the compose cluster and a working {@code docker} CLI:
 * <pre>
 * cd example/spring-kafka-protobuf/src/main/resources &amp;&amp; docker compose up -d
 * </pre>
 *
 * @author Turgay Can
 */
@EnabledIf("com.inomera.telco.commons.springkafka.e2e.E2eSupport#brokerAvailable")
class KafkaChaosE2eTest {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaChaosE2eTest.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(90);

    // ------------------------------------------------------------- listeners

    /** Collects everything it is handed. */
    static final class Collector {
        final ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();

        @KafkaListener(groupId = "unused", topics = "unused")
        void onMessage(String message) {
            received.add(message);
        }
    }

    /** Throws for one payload with RETRY_FROM_BROKER, succeeds for everything else. */
    static final class PoisonAwareListener {
        final ConcurrentLinkedQueue<String> succeeded = new ConcurrentLinkedQueue<>();
        final AtomicInteger poisonAttempts = new AtomicInteger();

        @KafkaListener(groupId = "unused", topics = "unused", retry = KafkaListener.RETRY.RETRY_FROM_BROKER)
        void onMessage(String message) {
            if (message.startsWith("poison")) {
                poisonAttempts.incrementAndGet();
                throw new IllegalStateException("poison record: " + message);
            }
            succeeded.add(message);
        }
    }

    /** Blocks far longer than max.poll.interval.ms to see whether the poller survives it. */
    static final class SlowListener {
        final CopyOnWriteArrayList<String> completed = new CopyOnWriteArrayList<>();
        private final long blockMillis;

        SlowListener(long blockMillis) {
            this.blockMillis = blockMillis;
        }

        @KafkaListener(groupId = "unused", topics = "unused")
        void onMessage(String message) {
            try {
                Thread.sleep(blockMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            completed.add(message);
        }
    }

    // --------------------------------------------------------------- helpers

    private final List<KafkaMessageConsumer> started = new ArrayList<>();
    private final List<PollerThreadStateChecker> checkers = new ArrayList<>();
    private final List<String> topics = new ArrayList<>();

    private KafkaConsumerBuilder consumerBuilder(String groupId, String topic, Object bean,
                                                 OffsetCommitStrategy strategy, Properties properties) {
        final ListenerMethodRegistry registry = new ListenerMethodRegistry();
        E2eSupport.registerListener(registry, groupId, topic, String.class, bean, "onMessage");
        return KafkaConsumerBuilder.builder(registry)
                .properties(properties)
                .groupId(groupId)
                .topics(topic)
                .offsetCommitStrategy(strategy)
                .autoPartitionPause(true)
                // Registered for every chaos consumer, not just the self-heal test: without it
                // getOrCreateConsumerThreadStore() hands back a NoopConsumerThreadStore, so
                // pollerThreadOf() returns null and every "the thread is still alive" assertion
                // passes without checking anything.
                .threadStore(new PollerThreadStore())
                .valueDeserializer(new E2eSupport.StringPayloadDeserializer());
    }

    private KafkaMessageConsumer start(KafkaConsumerBuilder builder) {
        final KafkaMessageConsumer consumer = builder.build();
        consumer.start();
        started.add(consumer);
        return consumer;
    }

    private String newTopic(String prefix, int partitions) {
        return newTopic(prefix, partitions, (short) 1);
    }

    private String newTopic(String prefix, int partitions, short replicationFactor) {
        final String topic = prefix + "-" + System.nanoTime();
        E2eSupport.createTopic(topic, partitions, replicationFactor);
        topics.add(topic);
        return topic;
    }

    /**
     * Consumer properties with short timeouts. Chaos tests deliberately break connectivity, and
     * the client defaults (60s api timeout, 5 min metadata age) would make every one of them time
     * out before the client even noticed.
     */
    private static Properties resilientProperties() {
        final Properties properties = E2eSupport.consumerProperties();
        properties.put("default.api.timeout.ms", "8000");
        properties.put("request.timeout.ms", "4000");
        properties.put("reconnect.backoff.max.ms", "1000");
        properties.put("metadata.max.age.ms", "2000");
        return properties;
    }

    @BeforeAll
    static void ensureClusterIsWhole() {
        E2eSupport.restoreCluster();
    }

    /**
     * One method, because the order matters and JUnit does not promise one between several
     * {@code @AfterEach}: stop the liveness checkers before they can restart a poller we are
     * tearing down, then the consumers, then put the cluster back together — a test that failed
     * halfway through a {@code docker stop} must not leave the next one without brokers.
     */
    @AfterEach
    void tearDown() {
        checkers.forEach(PollerThreadStateChecker::close);
        checkers.clear();
        started.forEach(consumer -> {
            try {
                consumer.stop();
            } catch (Exception e) {
                LOG.warn("Could not stop a consumer: {}", e.getMessage());
            }
        });
        started.clear();
        E2eSupport.restoreCluster();
        topics.forEach(E2eSupport::deleteTopic);
        topics.clear();
    }

    @AfterAll
    static void leaveClusterUsable() {
        E2eSupport.restoreCluster();
    }

    // ----------------------------------------------------------------- tests

    @Test
    @DisplayName("RETRY_FROM_BROKER: the withheld record is actually re-delivered after a restart")
    void retryFromBrokerRedeliversTheWithheldRecord() {
        final String topic = newTopic("chaos-retry-redeliver", 1);
        final String group = "chaos-retry-redeliver-group";
        final TopicPartition partition = new TopicPartition(topic, 0);

        final PoisonAwareListener bean = new PoisonAwareListener();
        final KafkaMessageConsumer consumer = start(consumerBuilder(group, topic, bean,
                OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, resilientProperties()));

        // offset 0 good, offset 1 poison, offsets 2-3 good.
        E2eSupport.publishOne(topic, "k0", "good-0");
        E2eSupport.publishOne(topic, "k1", "poison-1");
        E2eSupport.publishOne(topic, "k2", "good-2");
        E2eSupport.publishOne(topic, "k3", "good-3");

        E2eSupport.await("every record to be attempted once", TIMEOUT,
                () -> bean.succeeded.size() >= 3 && bean.poisonAttempts.get() >= 1);
        E2eSupport.settle(Duration.ofSeconds(3));

        // This is the whole point. The poll loop kept going and processed offsets 2 and 3, but
        // the committed offset must not have moved past the poison record at offset 1.
        final long committed = E2eSupport.committedOffset(group, partition);
        assertTrue(committed <= 1L,
                "committed offset " + committed + " moved past the withheld record at offset 1; "
                        + "the record can never be re-delivered and is lost");

        consumer.stop();
        started.remove(consumer);

        // A fresh consumer in the same group must see the poison record again — that is what
        // "the broker re-delivers it" means, and what the offset was withheld for.
        final Collector afterRestart = new Collector();
        start(consumerBuilder(group, topic, afterRestart,
                OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, resilientProperties()));

        E2eSupport.await("the withheld record to be re-delivered", TIMEOUT,
                () -> afterRestart.received.stream().anyMatch(m -> m.equals("poison-1")));
    }

    @Test
    @DisplayName("a broker dying mid-consumption must not kill the poller or lose messages")
    void brokerRestartLosesNothing() {
        // Replicated across all three brokers: with the default factor of 1 the partitions that
        // broker-3 leads simply go offline when it stops, so the producer silently drops the
        // records aimed at them and the test measures the compose file, not the poller.
        final String topic = newTopic("chaos-broker-bounce", 3, (short) 3);
        final String group = "chaos-broker-bounce-group";

        final Collector bean = new Collector();
        start(consumerBuilder(group, topic, bean,
                OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, resilientProperties()));

        E2eSupport.publish(topic, "before-", 30);
        E2eSupport.await("the first batch", TIMEOUT, () -> distinct(bean.received, "before-") == 30);

        // Take a broker away underneath the consumer.
        E2eSupport.dockerStop("broker-3");
        E2eSupport.awaitPort(30000, false, Duration.ofSeconds(60));
        LOG.info("broker-3 stopped, publishing through the outage");

        E2eSupport.publish(topic, "during-", 30);
        E2eSupport.await("records published during the outage", TIMEOUT,
                () -> distinct(bean.received, "during-") == 30);

        E2eSupport.dockerStart("broker-3");
        E2eSupport.awaitPort(30000, true, Duration.ofSeconds(90));
        LOG.info("broker-3 back up");

        E2eSupport.publish(topic, "after-", 30);
        E2eSupport.await("records published after recovery", TIMEOUT,
                () -> distinct(bean.received, "after-") == 30);

        assertEquals(90, distinct(bean.received, ""),
                "expected every distinct payload exactly once at least; got " + bean.received.size()
                        + " deliveries of " + distinct(bean.received, "") + " distinct payloads");
    }

    @Test
    @DisplayName("a full cluster outage must not kill the poller thread; it recovers on its own")
    void fullOutageKeepsThePollerAlive() {
        final String topic = newTopic("chaos-full-outage", 1);
        final String group = "chaos-full-outage-group";

        final Collector bean = new Collector();
        final KafkaMessageConsumer consumer = start(consumerBuilder(group, topic, bean,
                OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, resilientProperties()));

        E2eSupport.publish(topic, "pre-", 10);
        E2eSupport.await("the pre-outage batch", TIMEOUT, () -> distinct(bean.received, "pre-") == 10);

        final Thread pollerThread = pollerThreadOf(group);

        E2eSupport.BROKER_CONTAINERS.forEach(E2eSupport::dockerStop);
        E2eSupport.BROKER_PORTS.forEach(port -> E2eSupport.awaitPort(port, false, Duration.ofSeconds(60)));
        LOG.info("whole cluster down");

        // Ride out the outage well past request.timeout.ms and default.api.timeout.ms. Every
        // network failure surfaces inside poll(); if any of them escaped the loop the thread
        // would be gone by now and consumption would never resume.
        E2eSupport.settle(Duration.ofSeconds(20));
        assertNotNull(pollerThread, "no poller thread was registered");
        assertTrue(pollerThread.isAlive(), "the poller thread died during the outage");
        assertTrue(consumer.isRunning(), "the consumer reported itself as stopped during the outage");

        E2eSupport.restoreCluster();
        LOG.info("cluster restored");

        E2eSupport.publish(topic, "post-", 10);
        E2eSupport.await("consumption to resume after the outage", TIMEOUT,
                () -> distinct(bean.received, "post-") == 10);
    }

    @Test
    @DisplayName("offsets committed while the cluster is down are not lost once it returns")
    void commitFailuresDuringAnOutageDoNotLoseOffsets() {
        final String topic = newTopic("chaos-commit-failure", 1);
        final String group = "chaos-commit-failure-group";
        final TopicPartition partition = new TopicPartition(topic, 0);

        final Collector bean = new Collector();
        start(consumerBuilder(group, topic, bean,
                OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, resilientProperties()));

        E2eSupport.publish(topic, "settled-", 5);
        E2eSupport.await("the settled batch", TIMEOUT, () -> distinct(bean.received, "settled-") == 5);
        E2eSupport.await("its offsets to be committed", TIMEOUT,
                () -> E2eSupport.committedOffset(group, partition) == 5L);

        // Records are already in the broker's log; the consumer will pick them up and then find
        // it has nobody to commit to.
        E2eSupport.publish(topic, "orphan-", 5);
        E2eSupport.BROKER_CONTAINERS.forEach(E2eSupport::dockerStop);
        E2eSupport.BROKER_PORTS.forEach(port -> E2eSupport.awaitPort(port, false, Duration.ofSeconds(60)));

        E2eSupport.settle(Duration.ofSeconds(15));

        E2eSupport.restoreCluster();

        // Whatever happened to the individual commitAsync calls, the group must end up committed
        // at the end of the log rather than stuck, and no record may be skipped.
        E2eSupport.await("offsets to catch up after the outage", TIMEOUT, () -> {
            final long committed = E2eSupport.committedOffset(group, partition);
            final long end = E2eSupport.endOffset(partition);
            return committed == end && end == 10L;
        });
        assertEquals(10, distinct(bean.received, ""), "a record was skipped across the outage");
    }

    @Test
    @DisplayName("a rebalance must not commit over a record the retry policy withheld")
    void rebalanceDoesNotCommitOverAWithheldRecord() {
        final String topic = newTopic("chaos-rebalance-pin", 2);
        final String group = "chaos-rebalance-pin-group";

        final PoisonAwareListener first = new PoisonAwareListener();
        start(consumerBuilder(group, topic, first,
                OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, resilientProperties()));

        // One poison record per partition, so whichever partition moves in the rebalance the
        // pinned offset is exercised.
        E2eSupport.publishOne(topic, "key-a", "poison-a");
        E2eSupport.publishOne(topic, "key-b", "poison-b");
        E2eSupport.publish(topic, "filler-", 20);

        E2eSupport.await("both poison records to be attempted", TIMEOUT,
                () -> first.poisonAttempts.get() >= 2);
        E2eSupport.settle(Duration.ofSeconds(2));

        // Force a rebalance by joining a second member.
        final PoisonAwareListener second = new PoisonAwareListener();
        start(consumerBuilder(group, topic, second,
                OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, resilientProperties()));
        E2eSupport.settle(Duration.ofSeconds(10));

        // Neither partition may be committed to its end: each still holds an unresolved record.
        // onPartitionsRevoked used to commit every finished future without asking whether it
        // succeeded, which is exactly what a rebalance triggers.
        boolean somePartitionStillHeldBack = false;
        for (int p = 0; p < 2; p++) {
            final TopicPartition partition = new TopicPartition(topic, p);
            final long committed = E2eSupport.committedOffset(group, partition);
            final long end = E2eSupport.endOffset(partition);
            LOG.info("{} committed={} end={}", partition, committed, end);
            if (committed < end) {
                somePartitionStillHeldBack = true;
            }
        }
        assertTrue(somePartitionStillHeldBack,
                "every partition was committed to the end even though two records were withheld");
    }

    @Test
    @DisplayName("self-heal: a poller thread that dies is restarted and resumes consuming")
    void selfHealRestartsADeadPoller() {
        final String topic = newTopic("chaos-self-heal", 1);
        final String group = "chaos-self-heal-group";

        final Properties checkerProperties = new Properties();
        checkerProperties.setProperty("poller.thread.checker.active", "true");
        checkerProperties.setProperty("poller.thread.checker.initialDelaySec", "2");
        checkerProperties.setProperty("poller.thread.checker.retryAsSec", "2");

        final PollerThreadStore threadStore = new PollerThreadStore();
        final Collector bean = new Collector();
        start(consumerBuilder(group, topic, bean,
                OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, resilientProperties())
                .threadStore(threadStore));

        final PollerThreadStateChecker checker = new PollerThreadStateChecker(threadStore, checkerProperties);
        checkers.add(checker);

        E2eSupport.publish(topic, "alive-", 5);
        E2eSupport.await("the consumer to be working", TIMEOUT, () -> distinct(bean.received, "alive-") == 5);

        final Thread pollerThread = pollerThreadOf(group);
        assertTrue(pollerThread != null && pollerThread.isAlive(), "no live poller thread was registered");

        // Interrupting the poller inside poll() raises Kafka's InterruptException, which unwinds
        // the loop exactly like an unexpected failure would: the thread ends with running=true
        // and closed=true, which is what shouldRestart() looks for.
        LOG.info("interrupting poller thread {}", pollerThread.getName());
        pollerThread.interrupt();

        E2eSupport.await("the poller thread to die", Duration.ofSeconds(30), () -> !pollerThread.isAlive());

        // The checker samples on a 2s timer and calls start() again.
        E2eSupport.publish(topic, "healed-", 5);
        E2eSupport.await("the restarted poller to consume again", TIMEOUT,
                () -> distinct(bean.received, "healed-") == 5);

        final Thread healedThread = pollerThreadOf(group);
        assertTrue(healedThread != null && healedThread.isAlive(), "no live poller thread after self-heal");
        assertFalse(healedThread == pollerThread, "the dead thread was never replaced");
    }

    @Test
    @DisplayName("a listener that blocks past max.poll.interval.ms must not kill the poller")
    void blockingListenerDoesNotKillThePoller() {
        final String topic = newTopic("chaos-blocking-listener", 1);
        final String group = "chaos-blocking-listener-group";

        final Properties properties = resilientProperties();
        // Deliberately shorter than the listener's block, so the consumer would be evicted from
        // the group if autoPartitionPause were not keeping poll() flowing.
        properties.put("max.poll.interval.ms", "6000");
        properties.put("max.poll.records", "1");

        final SlowListener bean = new SlowListener(9_000L);
        final KafkaMessageConsumer consumer = start(consumerBuilder(group, topic, bean,
                OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, properties));

        E2eSupport.publish(topic, "slow-", 3);

        E2eSupport.await("all three slow records to complete", TIMEOUT, () -> bean.completed.size() == 3);
        assertTrue(consumer.isRunning(), "the consumer stopped while a listener was blocking");

        final Thread pollerThread = pollerThreadOf(group);
        assertNotNull(pollerThread, "no poller thread was registered");
        assertTrue(pollerThread.isAlive(), "the poller thread died while a listener was blocking");
    }

    @Test
    @DisplayName("AT_MOST_ONCE_BULK: a refused pre-dispatch commit loses nothing and duplicates nothing")
    void atMostOnceCommitFailureLosesNothing() {
        final String topic = newTopic("chaos-atmostonce-commit", 1);
        final String group = "chaos-atmostonce-commit-group";
        final TopicPartition partition = new TopicPartition(topic, 0);

        final ConcurrentLinkedQueue<String> delivered = new ConcurrentLinkedQueue<>();
        final List<Long> deliveredOffsets = new CopyOnWriteArrayList<>();
        final Properties properties = resilientProperties();
        properties.put("group.id", group);

        final KafkaConsumerProperties consumerProperties = new KafkaConsumerProperties(
                group, List.of(topic), null, OffsetCommitStrategy.AT_MOST_ONCE_BULK, properties);

        // Fails the very first pre-dispatch commit, exactly as a STALE_MEMBER_EPOCH rejection
        // does. Whatever raised it, the poller's reaction is the same: return false from
        // dispatch and abandon the cycle.
        final AtomicInteger commitAttempts = new AtomicInteger();
        final DefaultConsumerPoller poller = new DefaultConsumerPoller(
                consumerProperties, new E2eSupport.StringPayloadDeserializer(), true) {
            @Override
            protected boolean commitAllSyncBeforeProcessing() {
                if (commitAttempts.getAndIncrement() == 0) {
                    LOG.info("simulating a rejected pre-dispatch commit");
                    return false;
                }
                return super.commitAllSyncBeforeProcessing();
            }
        };
        poller.setConsumerRecordHandler(record -> {
            delivered.add(String.valueOf(record.value()));
            deliveredOffsets.add(record.offset());
            return CompletableFuture.completedFuture(new InvokerResult(record));
        });

        E2eSupport.publish(topic, "batch-", 10);

        poller.start();
        try {
            E2eSupport.await("the commit to have been attempted and refused", TIMEOUT,
                    () -> commitAttempts.get() >= 1);
            E2eSupport.publish(topic, "later-", 10);
            E2eSupport.await("every record to arrive", TIMEOUT, () -> delivered.size() >= 20);
            E2eSupport.settle(Duration.ofSeconds(5));

            final long committed = E2eSupport.committedOffset(group, partition);
            final long end = E2eSupport.endOffset(partition);
            LOG.info("delivered={} committed={} end={}", deliveredOffsets.size(), committed, end);

            // No loss: poll() had already moved the fetch position past the refused batch, so
            // abandoning the cycle used to throw those records away for good. The rewind puts the
            // position back and the batch is read again.
            assertEquals(20, delivered.size(),
                    "records were lost or duplicated: " + deliveredOffsets);
            assertEquals(Set.copyOf(deliveredOffsets).size(), deliveredOffsets.size(),
                    "a record was delivered twice, which AT_MOST_ONCE must never do: " + deliveredOffsets);
            // And the offsets must not have run ahead of what was actually handled.
            assertTrue(committed <= end, "committed=" + committed + " end=" + end);
        } finally {
            poller.stop();
        }
    }

    // -------------------------------------------------- KIP-848 (production shape)

    /**
     * The exact combination pmp-cis runs: {@code group.protocol=consumer} plus
     * {@code AT_MOST_ONCE_BULK}. Under the consumer protocol the group coordinator bumps the
     * member epoch on every reconciliation, and an in-flight commit that misses the bump comes
     * back as {@code STALE_MEMBER_EPOCH}. The client will not retry it — the exception is an
     * {@code ApiException}, not a {@code RetriableException} — so the poller has to.
     */
    @Test
    @DisplayName("KIP-848 + AT_MOST_ONCE_BULK: coordinator loss must not kill the poller or lose records")
    void atMostOnceBulkSurvivesCoordinatorLossUnderKip848() {
        final String topic = newTopic("chaos-kip848-atmostonce", 1, (short) 3);
        final String group = "chaos-kip848-atmostonce-group";
        final TopicPartition partition = new TopicPartition(topic, 0);

        final Collector bean = new Collector();
        final KafkaMessageConsumer consumer = start(consumerBuilder(group, topic, bean,
                OffsetCommitStrategy.AT_MOST_ONCE_BULK, resilientProperties())
                .serverSideRebalance("uniform"));

        E2eSupport.publish(topic, "pre-", 20);
        E2eSupport.await("the pre-outage batch", TIMEOUT, () -> distinct(bean.received, "pre-") == 20);

        final Thread pollerThread = pollerThreadOf(group);

        // Taking every broker down takes the group coordinator with it, which is what turns the
        // pre-dispatch commitSync into a TimeoutException.
        E2eSupport.BROKER_CONTAINERS.forEach(E2eSupport::dockerStop);
        E2eSupport.BROKER_PORTS.forEach(port -> E2eSupport.awaitPort(port, false, Duration.ofSeconds(60)));
        E2eSupport.settle(Duration.ofSeconds(25));

        assertNotNull(pollerThread, "no poller thread was registered");
        assertTrue(pollerThread.isAlive(), "the poller thread died during the coordinator outage");
        assertTrue(consumer.isRunning(), "the consumer reported itself stopped during the outage");

        E2eSupport.restoreCluster();
        E2eSupport.publish(topic, "post-", 20);
        E2eSupport.await("consumption to resume", TIMEOUT, () -> distinct(bean.received, "post-") == 20);

        // AT_MOST_ONCE exists so that a record is never handled twice. That must hold across the
        // outage, whatever happened to the commits in the middle of it.
        final List<String> all = List.copyOf(bean.received);
        assertEquals(Set.copyOf(all).size(), all.size(),
                "a record was delivered more than once, which AT_MOST_ONCE must never do");
        LOG.info("kip848 outage: delivered={} committed={} end={}", all.size(),
                E2eSupport.committedOffset(group, partition), E2eSupport.endOffset(partition));
    }

    @Test
    @DisplayName("KIP-848 + AT_MOST_ONCE_BULK: member churn bumps the epoch and must not duplicate records")
    void atMostOnceBulkUnderKip848MemberChurn() {
        final String topic = newTopic("chaos-kip848-churn", 6, (short) 3);
        final String group = "chaos-kip848-churn-group";

        final Collector first = new Collector();
        start(consumerBuilder(group, topic, first, OffsetCommitStrategy.AT_MOST_ONCE_BULK,
                resilientProperties()).serverSideRebalance("uniform"));

        E2eSupport.publish(topic, "warm-", 30);
        E2eSupport.await("the warm-up batch", TIMEOUT, () -> distinct(first.received, "warm-") == 30);

        // Every join and leave makes the coordinator reconcile and bump member epochs, which is
        // the window a commit can land in and be refused.
        final Collector second = new Collector();
        final Collector third = new Collector();
        for (int round = 0; round < 3; round++) {
            final KafkaMessageConsumer joiner = start(consumerBuilder(group, topic,
                    round % 2 == 0 ? second : third, OffsetCommitStrategy.AT_MOST_ONCE_BULK,
                    resilientProperties()).serverSideRebalance("uniform"));
            E2eSupport.publish(topic, "churn" + round + "-", 30);
            E2eSupport.settle(Duration.ofSeconds(4));
            joiner.stop();
            started.remove(joiner);
            E2eSupport.settle(Duration.ofSeconds(4));
        }

        E2eSupport.settle(Duration.ofSeconds(8));

        final List<String> all = new ArrayList<>(first.received);
        all.addAll(second.received);
        all.addAll(third.received);
        LOG.info("kip848 churn: {} deliveries, {} distinct", all.size(), Set.copyOf(all).size());

        // The invariant that matters for this strategy: a record is committed before it is
        // handed over, so a member losing a partition can lose a record but must never cause the
        // same one to be processed twice.
        assertEquals(Set.copyOf(all).size(), all.size(),
                "a record was processed twice across the churn, which AT_MOST_ONCE must never do");

        // The surviving member has to still be consuming: an epoch-refused commit that unwound
        // the poll loop would leave the group alive in the coordinator's eyes but reading nothing.
        E2eSupport.publish(topic, "after-churn-", 20);
        E2eSupport.await("the surviving member to keep consuming after the churn", TIMEOUT,
                () -> distinct(first.received, "after-churn-") == 20);
    }

    // --------------------------------------------------------------- helpers

    /** Distinct payloads with the given prefix; deliveries may repeat under at-least-once. */
    private static long distinct(ConcurrentLinkedQueue<String> received, String prefix) {
        final Set<String> unique = received.stream()
                .filter(message -> message.startsWith(prefix))
                .collect(Collectors.toSet());
        return unique.size();
    }

    /** The live poller thread for a group, as recorded by {@link PollerThreadStore}. */
    private static Thread pollerThreadOf(String group) {
        return new PollerThreadStore().getThreads().values().stream()
                .map(PollerThreadState::getThread)
                .filter(thread -> thread != null && thread.getName().contains(group))
                .filter(Thread::isAlive)
                .findFirst()
                .orElseGet(() -> new PollerThreadStore().getThreads().values().stream()
                        .map(PollerThreadState::getThread)
                        .filter(thread -> thread != null && thread.getName().contains(group))
                        .findFirst()
                        .orElse(null));
    }
}
