package com.inomera.telco.commons.springkafka.e2e;

import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import com.inomera.telco.commons.springkafka.builder.KafkaConsumerBuilder;
import com.inomera.telco.commons.springkafka.builder.virtual.UnorderedVirtualProcessingStrategyBuilder;
import com.inomera.telco.commons.springkafka.builder.virtual.VirtualConsumerInvokerBuilder;
import com.inomera.telco.commons.springkafka.builder.virtual.VirtualKafkaConsumerBuilder;
import com.inomera.telco.commons.springkafka.consumer.KafkaMessageConsumer;
import com.inomera.telco.commons.springkafka.consumer.OffsetCommitStrategy;
import com.inomera.telco.commons.springkafka.consumer.invoker.ListenerMethodRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Consume-side throughput, one case per configuration.
 *
 * <p>Opt-in, because it takes minutes and needs a broker:
 *
 * <pre>
 * cd example/spring-kafka-protobuf/src/main/resources &amp;&amp; docker compose up -d
 * ./gradlew :spring-kafka:test --tests '*ThroughputBenchmarkTest*' -Pbenchmark=true
 * </pre>
 *
 * <h2>What is measured</h2>
 * The backlog is published <em>before</em> the consumer starts, so each case measures pure
 * consume throughput rather than producer behaviour. Timing runs from the first record handed to
 * a listener to the last, which excludes consumer construction and group join — otherwise the
 * couple of seconds spent joining would dominate the smaller cases.
 *
 * <p>Listeners are no-ops that only count, so the number reflects the framework rather than any
 * business logic.
 */
class ThroughputBenchmarkTest {

    private static final int PARTITIONS = 6;
    private static final Duration TIMEOUT = Duration.ofMinutes(5);
    private static final List<String> REPORT = new ArrayList<>();

    @BeforeAll
    static void requireOptIn() {
        assumeTrue(Boolean.parseBoolean(System.getProperty("benchmark", "false")),
                "Benchmarks are opt-in; run with -Pbenchmark=true");
        assumeTrue(E2eSupport.brokerAvailable(),
                "No Kafka broker on " + E2eSupport.BOOTSTRAP);
    }

    @AfterAll
    static void printReport() {
        if (REPORT.isEmpty()) {
            return;
        }
        final StringBuilder out = new StringBuilder("\n")
                .append("=".repeat(118)).append('\n')
                .append(String.format("%-44s %-14s %8s %9s %10s %9s %10s%n",
                        "CASE", "THREADS", "RECORDS", "DISP.ms", "DISP.TPS", "E2E.ms", "E2E.TPS"))
                .append("=".repeat(118)).append('\n');
        REPORT.forEach(line -> out.append(line).append('\n'));
        out.append("=".repeat(118)).append('\n')
                .append("DISP = first listener call -> last listener call (commits off the critical path\n")
                .append("       for at-least-once, so this is dispatch+listener throughput over prefetched data)\n")
                .append("E2E  = consumer.start() -> last record consumed (includes group join and every commit)");
        System.out.println(out);
    }

    private static void record(String caseName, String threads, int records, long dispatchMillis, long e2eMillis) {
        REPORT.add(String.format("%-44s %-14s %8d %9d %10s %9d %10s", caseName, threads, records,
                dispatchMillis, tps(records, dispatchMillis), e2eMillis, tps(records, e2eMillis)));
    }

    private static String tps(int records, long millis) {
        final double tps = millis <= 0 ? records : records * 1000.0d / millis;
        return tps >= 1000 ? String.format("%.1fK", tps / 1000) : String.format("%.0f", tps);
    }

    // ------------------------------------------------------------- listeners

    /** Counts records and stamps the first and last arrival. */
    static final class Counter {
        final AtomicInteger count = new AtomicInteger();
        final AtomicLong firstNanos = new AtomicLong();
        final AtomicLong lastNanos = new AtomicLong();

        void hit(int n) {
            final long now = System.nanoTime();
            firstNanos.compareAndSet(0L, now);
            lastNanos.set(now);
            count.addAndGet(n);
        }

        long elapsedMillis() {
            return (lastNanos.get() - firstNanos.get()) / 1_000_000L;
        }
    }

    static final class SingleListener {
        final Counter counter = new Counter();

        @KafkaListener(groupId = "unused", topics = "unused")
        void onMessage(String message) {
            counter.hit(1);
        }
    }

    static final class BulkListener {
        final Counter counter = new Counter();

        @KafkaListener(groupId = "unused", topics = "unused")
        @SuppressWarnings("rawtypes")
        void onMessages(Set messages) {
            counter.hit(messages.size());
        }
    }

    // --------------------------------------------------------------- runners

    /** Publishes a backlog, drains it with an OS-thread consumer, records the rate. */
    private static void runOsThreadCase(String caseName, OffsetCommitStrategy strategy, int records) {
        final String topic = "bench-" + System.nanoTime();
        final String group = topic + "-group";
        E2eSupport.createTopic(topic, PARTITIONS);
        E2eSupport.publish(topic, "m-", records);

        final SingleListener bean = new SingleListener();
        final ListenerMethodRegistry registry = new ListenerMethodRegistry();
        E2eSupport.registerListener(registry, group, topic, String.class, bean, "onMessage");

        final KafkaMessageConsumer consumer = KafkaConsumerBuilder.builder(registry)
                .properties(E2eSupport.consumerProperties())
                .groupId(group)
                .topics(topic)
                .offsetCommitStrategy(strategy)
                .valueDeserializer(new E2eSupport.StringPayloadDeserializer())
                .autoPartitionPause(true)
                .invoker()
                .unordered()
                .singleExecutor()
                .coreThreadCount(8)
                .maxThreadCount(16)
                .keepAliveTime(1)
                .keepAliveTimeUnit(TimeUnit.MINUTES)
                .queueCapacity(10_000)
                .and()
                .and()
                .and()
                .build();

        final long startedAt = System.nanoTime();
        consumer.start();
        try {
            E2eSupport.await(caseName, TIMEOUT, () -> bean.counter.count.get() >= records);
            final long e2eMillis = (System.nanoTime() - startedAt) / 1_000_000L;
            record(caseName, "OS x8-16", records, bean.counter.elapsedMillis(), e2eMillis);
        } finally {
            consumer.stop();
            E2eSupport.deleteTopic(topic);
        }
    }

    /** Same, but the bulk poller hands whole per-partition batches to the listener. */
    private static void runBulkCase(String caseName, OffsetCommitStrategy strategy, int records) {
        final String topic = "bench-bulk-" + System.nanoTime();
        final String group = topic + "-group";
        E2eSupport.createTopic(topic, PARTITIONS);
        E2eSupport.publish(topic, "m-", records);

        final BulkListener bean = new BulkListener();
        final ListenerMethodRegistry registry = new ListenerMethodRegistry();
        E2eSupport.registerListener(registry, group, topic, Set.class, bean, "onMessages");

        final KafkaMessageConsumer consumer = KafkaConsumerBuilder.builder(registry)
                .properties(E2eSupport.consumerProperties())
                .groupId(group)
                .topics(topic)
                .offsetCommitStrategy(strategy)
                .valueDeserializer(new E2eSupport.StringPayloadDeserializer())
                .autoPartitionPause(true)
                .invoker()
                .unordered()
                .singleExecutor()
                .coreThreadCount(8)
                .maxThreadCount(16)
                .keepAliveTime(1)
                .keepAliveTimeUnit(TimeUnit.MINUTES)
                .queueCapacity(10_000)
                .and()
                .and()
                .and()
                .buildBulk();

        final long startedAt = System.nanoTime();
        consumer.start();
        try {
            E2eSupport.await(caseName, TIMEOUT, () -> bean.counter.count.get() >= records);
            final long e2eMillis = (System.nanoTime() - startedAt) / 1_000_000L;
            record(caseName, "OS x8-16", records, bean.counter.elapsedMillis(), e2eMillis);
        } finally {
            consumer.stop();
            E2eSupport.deleteTopic(topic);
        }
    }

    /** Virtual threads. {@code cap <= 0} selects the unbounded per-task executor. */
    private static void runVirtualCase(String caseName, OffsetCommitStrategy strategy, int records, int cap) {
        final String topic = "bench-virtual-" + System.nanoTime();
        final String group = topic + "-group";
        E2eSupport.createTopic(topic, PARTITIONS);
        E2eSupport.publish(topic, "m-", records);

        final SingleListener bean = new SingleListener();
        final ListenerMethodRegistry registry = new ListenerMethodRegistry();
        E2eSupport.registerListener(registry, group, topic, String.class, bean, "onMessage");

        final UnorderedVirtualProcessingStrategyBuilder unordered = VirtualKafkaConsumerBuilder.builder(registry)
                .properties(E2eSupport.consumerProperties())
                .groupId(group)
                .topics(topic)
                .offsetCommitStrategy(strategy)
                .valueDeserializer(new E2eSupport.StringPayloadDeserializer())
                .autoPartitionPause(true)
                .invoker()
                .unordered();

        final VirtualConsumerInvokerBuilder invoker =
                cap > 0 ? unordered.boundedExecutor(cap) : unordered.singleExecutor().and().and();

        final KafkaMessageConsumer consumer = invoker.and().build();

        final long startedAt = System.nanoTime();
        consumer.start();
        try {
            E2eSupport.await(caseName, TIMEOUT, () -> bean.counter.count.get() >= records);
            final long e2eMillis = (System.nanoTime() - startedAt) / 1_000_000L;
            record(caseName, cap > 0 ? "virtual cap=" + cap : "virtual ∞", records,
                    bean.counter.elapsedMillis(), e2eMillis);
        } finally {
            consumer.stop();
            E2eSupport.deleteTopic(topic);
        }
    }

    // ----------------------------------------------------------------- cases

    @Test
    @DisplayName("throughput matrix")
    void throughputMatrix() {
        // At-most-once commits synchronously before dispatch, so these runs are dominated by
        // broker round trips and 20k records already take seconds.
        final int atMostOnce = 20_000;
        // At-least-once keeps commits off the critical path, so a no-op listener drains 20k in
        // tens of milliseconds — far too short to measure. Use a backlog big enough that the run
        // lasts seconds and the number stops being scheduling noise.
        final int atLeastOnce = 200_000;
        // A synchronous commit per record; anything larger would run for many minutes.
        final int atMostOnceSingle = 1_000;

        runOsThreadCase("single-record / AT_MOST_ONCE_BULK",
                OffsetCommitStrategy.AT_MOST_ONCE_BULK, atMostOnce);
        runOsThreadCase("single-record / AT_LEAST_ONCE_BULK",
                OffsetCommitStrategy.AT_LEAST_ONCE_BULK, atLeastOnce);
        runOsThreadCase("single-record / AT_LEAST_ONCE_SINGLE",
                OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, atLeastOnce);
        runOsThreadCase("single-record / AT_MOST_ONCE_SINGLE",
                OffsetCommitStrategy.AT_MOST_ONCE_SINGLE, atMostOnceSingle);

        runBulkCase("bulk / AT_MOST_ONCE_BULK", OffsetCommitStrategy.AT_MOST_ONCE_BULK, atMostOnce);
        runBulkCase("bulk / AT_LEAST_ONCE_BULK", OffsetCommitStrategy.AT_LEAST_ONCE_BULK, atLeastOnce);

        runVirtualCase("virtual / AT_LEAST_ONCE_BULK / unbounded",
                OffsetCommitStrategy.AT_LEAST_ONCE_BULK, atLeastOnce, 0);
        runVirtualCase("virtual / AT_LEAST_ONCE_BULK / cap=64",
                OffsetCommitStrategy.AT_LEAST_ONCE_BULK, atLeastOnce, 64);
        runVirtualCase("virtual / AT_LEAST_ONCE_BULK / cap=8",
                OffsetCommitStrategy.AT_LEAST_ONCE_BULK, atLeastOnce, 8);
        runVirtualCase("virtual / AT_LEAST_ONCE_SINGLE / cap=64",
                OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, atLeastOnce, 64);
    }
}
