package com.inomera.telco.commons.springkafka.e2e;

import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import com.inomera.telco.commons.springkafka.builder.virtual.VirtualKafkaConsumerBuilder;
import com.inomera.telco.commons.springkafka.consumer.KafkaMessageConsumer;
import com.inomera.telco.commons.springkafka.consumer.OffsetCommitStrategy;
import com.inomera.telco.commons.springkafka.consumer.invoker.ListenerMethodRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Diagnostic for the open finding in PERFORMANCE.md: under {@code -XX:+UseZGC} the unbounded
 * virtual dispatcher failed to drain a 200k backlog within five minutes, while the same case took
 * well under a second under G1 and Parallel.
 *
 * <p>Prints progress, and on a stall dumps platform-thread states plus the poller's stack, which
 * is what distinguishes "starved of capacity" from "wedged waiting on something".
 *
 * <pre>
 * ./gradlew :spring-kafka:test --tests '*UnboundedVirtualStallDiagnosticTest*' \
 *     -Pbenchmark=true -PbenchmarkJvmArgs="-XX:+UseZGC -Xmx1g"
 * </pre>
 */
class UnboundedVirtualStallDiagnosticTest {

    private static final int RECORDS = Integer.getInteger("stall.records", 200_000);
    private static final Duration BUDGET = Duration.ofSeconds(120);

    @BeforeAll
    static void requireOptIn() {
        assumeTrue(Boolean.parseBoolean(System.getProperty("benchmark", "false")),
                "Diagnostic is opt-in; run with -Pbenchmark=true");
        assumeTrue(E2eSupport.brokerAvailable(), "No Kafka broker on " + E2eSupport.BOOTSTRAP);
    }

    static final class Counter {
        final AtomicInteger count = new AtomicInteger();

        @KafkaListener(groupId = "unused", topics = "unused")
        void onMessage(String message) {
            count.incrementAndGet();
        }
    }

    @Test
    @DisplayName("unbounded virtual dispatcher drains a large backlog")
    void drainsLargeBacklog() {
        final String topic = "unbounded-stall-" + System.nanoTime();
        final String group = topic + "-group";
        E2eSupport.createTopic(topic, 6);
        E2eSupport.publish(topic, "m-", RECORDS);

        final Counter bean = new Counter();
        final ListenerMethodRegistry registry = new ListenerMethodRegistry();
        E2eSupport.registerListener(registry, group, topic, String.class, bean, "onMessage");

        final KafkaMessageConsumer consumer = VirtualKafkaConsumerBuilder.builder(registry)
                .properties(E2eSupport.consumerProperties())
                .groupId(group)
                .topics(topic)
                .offsetCommitStrategy(OffsetCommitStrategy.AT_LEAST_ONCE_BULK)
                .valueDeserializer(new E2eSupport.StringPayloadDeserializer())
                .autoPartitionPause(true)
                .invoker()
                .unordered()
                .singleExecutor()
                .and()
                .and()
                .and()
                .build();

        System.out.printf("collector=%s heapMax=%dMB records=%d%n",
                GcProbe.collectorLabel(), Runtime.getRuntime().maxMemory() / (1024 * 1024), RECORDS);

        consumer.start();
        try {
            final Instant deadline = Instant.now().plus(BUDGET);
            int previous = -1;
            int stalledSamples = 0;
            boolean dumped = false;

            while (Instant.now().isBefore(deadline)) {
                final int current = bean.count.get();
                if (current >= RECORDS) {
                    break;
                }
                System.out.printf("consumed=%d/%d platformThreads=%d%n",
                        current, RECORDS, ManagementFactory.getThreadMXBean().getThreadCount());

                stalledSamples = current == previous ? stalledSamples + 1 : 0;
                previous = current;

                if (stalledSamples == 3 && !dumped) {
                    dumped = true;
                    dumpThreads();
                }
                sleep(2_000);
            }

            final int consumed = bean.count.get();
            System.out.printf("FINAL consumed=%d/%d stalledSamples=%d%n", consumed, RECORDS, stalledSamples);
            if (consumed < RECORDS) {
                dumpThreads();
            }
            assertTrue(consumed >= RECORDS,
                    "only " + consumed + "/" + RECORDS + " consumed within " + BUDGET);
        } finally {
            consumer.stop();
            E2eSupport.deleteTopic(topic);
        }
    }

    /** Platform-thread state histogram plus the poller and retry threads' stacks. */
    private static void dumpThreads() {
        final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        final Map<String, Integer> byState = new TreeMap<>();
        final StringBuilder interesting = new StringBuilder();

        for (ThreadInfo info : bean.dumpAllThreads(false, false)) {
            if (info == null) {
                continue;
            }
            byState.merge(info.getThreadState().name(), 1, Integer::sum);
            final String name = info.getThreadName();
            if (name.contains("consumer-poller") || name.contains("rejection-retry")
                    || name.contains("memory-retryer")) {
                interesting.append(String.format("  %s [%s]%n", name, info.getThreadState()));
                final StackTraceElement[] stack = info.getStackTrace();
                for (int i = 0; i < Math.min(8, stack.length); i++) {
                    interesting.append("      at ").append(stack[i]).append('\n');
                }
            }
        }

        System.out.println("---- thread state histogram (platform threads) ----");
        byState.forEach((state, count) -> System.out.printf("  %-14s %d%n", state, count));
        System.out.println("---- poller / retry threads ----");
        System.out.println(interesting.isEmpty() ? "  (none found)" : interesting);
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
