package com.inomera.telco.commons.springkafka.e2e;

import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import com.inomera.telco.commons.springkafka.builder.KafkaConsumerBuilder;
import com.inomera.telco.commons.springkafka.builder.virtual.VirtualKafkaConsumerBuilder;
import com.inomera.telco.commons.springkafka.consumer.KafkaMessageConsumer;
import com.inomera.telco.commons.springkafka.consumer.OffsetCommitStrategy;
import com.inomera.telco.commons.springkafka.consumer.invoker.ListenerMethodRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end tests against a real Kafka 4 cluster.
 *
 * <p>Each test corresponds to a failure mode that 4.1.0 fixes; the comment on each one records
 * what the pre-4.1.0 code did. Start the broker first — see {@link E2eSupport}. Without one the
 * whole class skips.
 */
class KafkaE2eTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(45);

    @BeforeAll
    static void requireBroker() {
        assumeTrue(E2eSupport.brokerAvailable(),
                "No Kafka broker on " + E2eSupport.BOOTSTRAP + "; start docker compose to run these tests");
    }

    // ------------------------------------------------------------- listeners

    /** Records every payload it sees. */
    static final class Collector {
        final ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();

        @KafkaListener(groupId = "unused", topics = "unused")
        void onMessage(String message) {
            received.add(message);
        }
    }

    /** Slow listener, so a consumer can be stopped with work still in flight. */
    static final class SlowCollector {
        final ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();
        volatile long delayMillis = 25L;

        @KafkaListener(groupId = "unused", topics = "unused")
        void onMessage(String message) {
            received.add(message);
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /** Bulk counterpart. */
    static final class BulkCollector {
        final ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();
        final AtomicInteger batches = new AtomicInteger();

        @KafkaListener(groupId = "unused", topics = "unused")
        @SuppressWarnings("unchecked")
        void onMessages(Set messages) {
            batches.incrementAndGet();
            for (Object message : messages) {
                received.add(String.valueOf(message));
            }
        }
    }

    /** Always throws, so retry policies can be observed. */
    static final class AlwaysFailing {
        final CopyOnWriteArrayList<Long> attemptNanos = new CopyOnWriteArrayList<>();

        @KafkaListener(groupId = "unused", topics = "unused",
                retry = KafkaListener.RETRY.RETRY_IN_MEMORY_TASK, retryCount = 3, retryBackoffTime = 300L)
        void onMessage(String message) {
            attemptNanos.add(System.nanoTime());
            throw new IllegalStateException("always fails: " + message);
        }
    }

    /** Throws with RETRY_FROM_BROKER for one specific payload, succeeds for the rest. */
    static final class FailsOnPoison {
        final ConcurrentLinkedQueue<String> succeeded = new ConcurrentLinkedQueue<>();
        final AtomicInteger failures = new AtomicInteger();

        @KafkaListener(groupId = "unused", topics = "unused", retry = KafkaListener.RETRY.RETRY_FROM_BROKER)
        void onMessage(String message) {
            if (message.startsWith("poison")) {
                failures.incrementAndGet();
                throw new IllegalStateException("poison record");
            }
            succeeded.add(message);
        }
    }

    /** Tracks peak concurrency to prove the bounded dispatcher caps it. */
    static final class ConcurrencyProbe {
        final AtomicInteger inFlight = new AtomicInteger();
        final AtomicInteger peak = new AtomicInteger();
        final AtomicInteger handled = new AtomicInteger();

        @KafkaListener(groupId = "unused", topics = "unused")
        void onMessage(String message) {
            final int current = inFlight.incrementAndGet();
            peak.accumulateAndGet(current, Math::max);
            try {
                Thread.sleep(40);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                handled.incrementAndGet();
                inFlight.decrementAndGet();
            }
        }
    }

    // --------------------------------------------------------------- helpers

    private static KafkaMessageConsumer singleRecordConsumer(String groupId, String topic,
                                                             OffsetCommitStrategy strategy,
                                                             Object bean, String method,
                                                             org.apache.kafka.common.serialization.Deserializer<?> deserializer) {
        final ListenerMethodRegistry registry = new ListenerMethodRegistry();
        E2eSupport.registerListener(registry, groupId, topic, String.class, bean, method);
        return KafkaConsumerBuilder.builder(registry)
                .properties(E2eSupport.consumerProperties())
                .groupId(groupId)
                .topics(topic)
                .offsetCommitStrategy(strategy)
                .valueDeserializer(deserializer)
                .autoPartitionPause(true)
                .invoker()
                .unordered()
                .singleExecutor()
                .coreThreadCount(2)
                .maxThreadCount(4)
                .keepAliveTime(1)
                .keepAliveTimeUnit(TimeUnit.MINUTES)
                .queueCapacity(200)
                .and()
                .and()
                .and()
                .build();
    }

    private static KafkaMessageConsumer bulkConsumer(String groupId, String topic,
                                                     OffsetCommitStrategy strategy, BulkCollector bean) {
        final ListenerMethodRegistry registry = new ListenerMethodRegistry();
        E2eSupport.registerListener(registry, groupId, topic, Set.class, bean, "onMessages");
        return KafkaConsumerBuilder.builder(registry)
                .properties(E2eSupport.consumerProperties())
                .groupId(groupId)
                .topics(topic)
                .offsetCommitStrategy(strategy)
                .valueDeserializer(new E2eSupport.StringPayloadDeserializer())
                .autoPartitionPause(true)
                .invoker()
                .unordered()
                .singleExecutor()
                .coreThreadCount(2)
                .maxThreadCount(4)
                .keepAliveTime(1)
                .keepAliveTimeUnit(TimeUnit.MINUTES)
                .queueCapacity(200)
                .and()
                .and()
                .and()
                .buildBulk();
    }

    // ----------------------------------------------------------------- tests

    @Test
    @DisplayName("bulk: restarting the consumer must not re-deliver the batch")
    void bulkCommitsWholeBatch() {
        final String topic = "e2e-bulk-offset-" + System.nanoTime();
        final String group = "e2e-bulk-group";
        E2eSupport.createTopic(topic, 1);
        E2eSupport.publish(topic, "msg-", 40);

        final BulkCollector first = new BulkCollector();
        final KafkaMessageConsumer consumer =
                bulkConsumer(group, topic, OffsetCommitStrategy.AT_LEAST_ONCE_BULK, first);
        consumer.start();
        try {
            E2eSupport.await("40 records", TIMEOUT, () -> first.received.size() >= 40);
        } finally {
            consumer.stop();
        }

        // Pre-4.1.0 committed the *lowest* offset of each batch, so a restart replayed almost
        // the entire batch. With max.poll.records=100 that meant ~39 duplicates here.
        final BulkCollector second = new BulkCollector();
        final KafkaMessageConsumer restarted =
                bulkConsumer(group, topic, OffsetCommitStrategy.AT_LEAST_ONCE_BULK, second);
        restarted.start();
        try {
            E2eSupport.settle(Duration.ofSeconds(6));
            assertEquals(0, second.received.size(),
                    "restart re-delivered " + second.received.size() + " records; batch offsets were wrong");
        } finally {
            restarted.stop();
            E2eSupport.deleteTopic(topic);
        }
    }

    @Test
    @DisplayName("KIP-848 cooperative rebalance: a joining consumer must not stall the group")
    void cooperativeRebalanceKeepsConsuming() {
        final String topic = "e2e-coop-" + System.nanoTime();
        final String group = "e2e-coop-group";
        E2eSupport.createTopic(topic, 6);

        final Properties kip848 = E2eSupport.consumerProperties();
        // The new consumer group protocol assigns partitions incrementally, so
        // onPartitionsAssigned only carries the newly added ones.
        kip848.put("group.protocol", "consumer");

        final Collector firstBean = new Collector();
        final ListenerMethodRegistry firstRegistry = new ListenerMethodRegistry();
        E2eSupport.registerListener(firstRegistry, group, topic, String.class, firstBean, "onMessage");
        final KafkaMessageConsumer first = KafkaConsumerBuilder.builder(firstRegistry)
                .properties(kip848)
                .groupId(group)
                .topics(topic)
                .offsetCommitStrategy(OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE)
                .valueDeserializer(new E2eSupport.StringPayloadDeserializer())
                .autoPartitionPause(true)
                .invoker().unordered().singleExecutor()
                .coreThreadCount(2).maxThreadCount(4).keepAliveTime(1)
                .keepAliveTimeUnit(TimeUnit.MINUTES).queueCapacity(200)
                .and().and().and()
                .build();

        final Collector secondBean = new Collector();
        final ListenerMethodRegistry secondRegistry = new ListenerMethodRegistry();
        E2eSupport.registerListener(secondRegistry, group, topic, String.class, secondBean, "onMessage");
        final Properties kip848Second = E2eSupport.consumerProperties();
        kip848Second.put("group.protocol", "consumer");
        final KafkaMessageConsumer second = KafkaConsumerBuilder.builder(secondRegistry)
                .properties(kip848Second)
                .groupId(group)
                .topics(topic)
                .offsetCommitStrategy(OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE)
                .valueDeserializer(new E2eSupport.StringPayloadDeserializer())
                .autoPartitionPause(true)
                .invoker().unordered().singleExecutor()
                .coreThreadCount(2).maxThreadCount(4).keepAliveTime(1)
                .keepAliveTimeUnit(TimeUnit.MINUTES).queueCapacity(200)
                .and().and().and()
                .build();

        first.start();
        try {
            E2eSupport.publish(topic, "before-", 30);
            E2eSupport.await("first batch", TIMEOUT, () -> firstBean.received.size() >= 30);

            // Second member joins: pre-4.1.0 the incremental assignment wiped in-flight state,
            // the poller NPE'd on the next record and the thread died.
            second.start();
            E2eSupport.settle(Duration.ofSeconds(5));

            E2eSupport.publish(topic, "after-", 30);
            E2eSupport.await("second batch across both members", TIMEOUT, () -> {
                final long after = firstBean.received.stream().filter(m -> m.startsWith("after-")).count()
                        + secondBean.received.stream().filter(m -> m.startsWith("after-")).count();
                return after >= 30;
            });
        } finally {
            first.stop();
            second.stop();
            E2eSupport.deleteTopic(topic);
        }
    }

    @Test
    @DisplayName("RETRY_FROM_BROKER must not kill the poller")
    void retryFromBrokerKeepsPollerAlive() {
        final String topic = "e2e-retry-broker-" + System.nanoTime();
        final String group = "e2e-retry-broker-group";
        E2eSupport.createTopic(topic, 1);

        final FailsOnPoison bean = new FailsOnPoison();
        final KafkaMessageConsumer consumer = singleRecordConsumer(group, topic,
                OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, bean, "onMessage",
                new E2eSupport.StringPayloadDeserializer());

        consumer.start();
        try {
            E2eSupport.publishOne(topic, "k-poison", "poison-1");
            E2eSupport.await("the failing record to be attempted", TIMEOUT, () -> bean.failures.get() >= 1);

            // Pre-4.1.0 the retryer threw RetriableCommitFailedException out of the poll loop,
            // the poller closed its consumer and nothing after this point was ever consumed.
            E2eSupport.publishOne(topic, "k-ok", "good-1");
            E2eSupport.await("consumer still alive after the failure", TIMEOUT,
                    () -> bean.succeeded.stream().anyMatch(m -> m.equals("good-1")));

            assertTrue(bean.failures.get() >= 1, "expected the poison record to fail at least once");
        } finally {
            consumer.stop();
            E2eSupport.deleteTopic(topic);
        }
    }

    @Test
    @DisplayName("in-memory retry: attempts are spaced by backoff and bounded by retryCount")
    void inMemoryRetryAppliesBackoff() {
        final String topic = "e2e-retry-mem-" + System.nanoTime();
        final String group = "e2e-retry-mem-group";
        E2eSupport.createTopic(topic, 1);

        final AlwaysFailing bean = new AlwaysFailing();
        final KafkaMessageConsumer consumer = singleRecordConsumer(group, topic,
                OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, bean, "onMessage",
                new E2eSupport.StringPayloadDeserializer());

        consumer.start();
        try {
            E2eSupport.publishOne(topic, "k", "fail-me");

            // retryCount = 3: the initial delivery plus retries, then dead-lettered.
            E2eSupport.await("at least 2 attempts", TIMEOUT, () -> bean.attemptNanos.size() >= 2);
            E2eSupport.settle(Duration.ofSeconds(8));

            final List<Long> attempts = List.copyOf(bean.attemptNanos);
            assertTrue(attempts.size() >= 2, "expected retries, saw " + attempts.size());

            // Pre-4.1.0 retryBackoffTime was only consulted in the exit condition, never as a
            // delay, so all attempts landed within microseconds of each other.
            final long gapMillis = (attempts.get(1) - attempts.get(0)) / 1_000_000L;
            assertTrue(gapMillis >= 100L,
                    "attempts were " + gapMillis + "ms apart; backoff was not applied");

            // Bounded: attempts must not grow without limit.
            assertTrue(attempts.size() <= 6,
                    "expected retries to stop, saw " + attempts.size() + " attempts");
        } finally {
            consumer.stop();
            E2eSupport.deleteTopic(topic);
        }
    }

    @Test
    @DisplayName("a record the deserializer cannot decode must not stop the consumer")
    void poisonPayloadDoesNotStopConsumer() {
        final String topic = "e2e-poison-" + System.nanoTime();
        final String group = "e2e-poison-group";
        E2eSupport.createTopic(topic, 1);

        final Collector bean = new Collector();
        final KafkaMessageConsumer consumer = singleRecordConsumer(group, topic,
                OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, bean, "onMessage",
                new E2eSupport.PoisonAwareDeserializer("UNDECODABLE"));

        consumer.start();
        try {
            E2eSupport.publishOne(topic, "k1", "good-before");
            E2eSupport.publishOne(topic, "k2", "UNDECODABLE-payload");
            E2eSupport.publishOne(topic, "k3", "good-after");

            E2eSupport.await("records either side of the poison one", TIMEOUT,
                    () -> bean.received.contains("good-before") && bean.received.contains("good-after"));
            assertTrue(bean.received.stream().noneMatch(m -> m.contains("UNDECODABLE")),
                    "the undecodable record must not reach the listener");
        } finally {
            consumer.stop();
            E2eSupport.deleteTopic(topic);
        }
    }

    @Test
    @DisplayName("disaster: abruptly stopping a consumer mid-flight must not lose messages")
    void noMessageLossOnAbruptStop() {
        final String topic = "e2e-noloss-" + System.nanoTime();
        final String group = "e2e-noloss-group";
        final int total = 200;
        E2eSupport.createTopic(topic, 3);
        E2eSupport.publish(topic, "m-", total);

        // A slow listener guarantees the consumer is stopped with work still in flight.
        final SlowCollector firstBean = new SlowCollector();
        final KafkaMessageConsumer first = singleRecordConsumer(group, topic,
                OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, firstBean, "onMessage",
                new E2eSupport.StringPayloadDeserializer());

        first.start();
        try {
            E2eSupport.await("partial consumption", TIMEOUT, () -> firstBean.received.size() >= 20);
        } finally {
            first.stop();
        }

        final SlowCollector secondBean = new SlowCollector();
        secondBean.delayMillis = 0;
        final KafkaMessageConsumer second = singleRecordConsumer(group, topic,
                OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, secondBean, "onMessage",
                new E2eSupport.StringPayloadDeserializer());
        second.start();
        try {
            final java.util.Set<String> seen = new java.util.HashSet<>(firstBean.received);
            E2eSupport.await("every message to be delivered at least once", TIMEOUT, () -> {
                seen.addAll(secondBean.received);
                return seen.size() >= total;
            });
            // At-least-once: duplicates are allowed, gaps are not.
            assertEquals(total, seen.size(), "messages were lost across the restart");
        } finally {
            second.stop();
            E2eSupport.deleteTopic(topic);
        }
    }

    @Test
    @DisplayName("disaster: killing one group member reassigns its partitions to the survivor")
    void survivorTakesOverAfterMemberDies() {
        final String topic = "e2e-failover-" + System.nanoTime();
        final String group = "e2e-failover-group";
        E2eSupport.createTopic(topic, 4);

        final Collector aBean = new Collector();
        final Collector bBean = new Collector();
        final KafkaMessageConsumer a = singleRecordConsumer(group, topic,
                OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, aBean, "onMessage",
                new E2eSupport.StringPayloadDeserializer());
        final KafkaMessageConsumer b = singleRecordConsumer(group, topic,
                OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, bBean, "onMessage",
                new E2eSupport.StringPayloadDeserializer());

        a.start();
        b.start();
        try {
            E2eSupport.publish(topic, "phase1-", 40);
            E2eSupport.await("phase 1 across both members", TIMEOUT,
                    () -> aBean.received.size() + bBean.received.size() >= 40);

            // Member A dies; B must pick up A's partitions and keep consuming.
            a.stop();

            E2eSupport.publish(topic, "phase2-", 40);
            E2eSupport.await("phase 2 on the survivor", TIMEOUT, () -> {
                final long phase2 = aBean.received.stream().filter(m -> m.startsWith("phase2-")).count()
                        + bBean.received.stream().filter(m -> m.startsWith("phase2-")).count();
                return phase2 >= 40;
            });
        } finally {
            b.stop();
            E2eSupport.deleteTopic(topic);
        }
    }

    @Test
    @DisplayName("bounded virtual dispatcher caps concurrent invocations")
    void boundedVirtualDispatcherCapsConcurrency() {
        final String topic = "e2e-bounded-" + System.nanoTime();
        final String group = "e2e-bounded-group";
        final int cap = 4;
        E2eSupport.createTopic(topic, 3);

        final ConcurrencyProbe bean = new ConcurrencyProbe();
        final ListenerMethodRegistry registry = new ListenerMethodRegistry();
        E2eSupport.registerListener(registry, group, topic, String.class, bean, "onMessage");

        final KafkaMessageConsumer consumer = VirtualKafkaConsumerBuilder.builder(registry)
                .properties(E2eSupport.consumerProperties())
                .groupId(group)
                .topics(topic)
                .offsetCommitStrategy(OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE)
                .valueDeserializer(new E2eSupport.StringPayloadDeserializer())
                .autoPartitionPause(true)
                .invoker()
                .unordered()
                .boundedExecutor(cap)
                .and()
                .build();

        consumer.start();
        try {
            E2eSupport.publish(topic, "burst-", 120);
            E2eSupport.await("most of the burst to be handled", TIMEOUT, () -> bean.handled.get() >= 100);

            // A plain thread-per-task virtual executor never rejects, so without the semaphore
            // an entire poll batch runs at once and the downstream absorbs the whole burst.
            assertTrue(bean.peak.get() <= cap,
                    "peak concurrency was " + bean.peak.get() + ", cap was " + cap);
            assertTrue(bean.peak.get() > 1, "expected real concurrency, peak was " + bean.peak.get());
        } finally {
            consumer.stop();
            E2eSupport.deleteTopic(topic);
        }
    }
}
