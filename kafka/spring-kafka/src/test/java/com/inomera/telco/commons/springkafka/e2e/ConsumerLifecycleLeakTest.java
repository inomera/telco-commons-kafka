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

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Do repeated consumer start/stop cycles release their threads?
 *
 * <p>Written while chasing the stall recorded in PERFORMANCE.md: the unbounded virtual dispatcher
 * only stalled as the seventh case in a shared JVM, never in isolation, which points at state
 * accumulating across consumer lifecycles rather than at the collector. That pattern matters in
 * production too — {@code PollerThreadStateChecker} restarts pollers, and an application context
 * refresh recreates consumers.
 */
class ConsumerLifecycleLeakTest {

    private static final int CYCLES = Integer.getInteger("leak.cycles", 8);
    private static final int RECORDS_PER_CYCLE = 500;
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

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
    @DisplayName("repeated OS-thread consumer lifecycles do not leak threads")
    void osThreadLifecyclesDoNotLeak() {
        runCycles(false);
    }

    @Test
    @DisplayName("repeated virtual consumer lifecycles do not leak threads")
    void virtualLifecyclesDoNotLeak() {
        runCycles(true);
    }

    private void runCycles(boolean virtual) {
        final String topic = "leak-" + (virtual ? "virtual-" : "os-") + System.nanoTime();
        E2eSupport.createTopic(topic, 6);

        final List<Integer> threadCounts = new ArrayList<>();
        try {
            for (int cycle = 1; cycle <= CYCLES; cycle++) {
                final String group = topic + "-group-" + cycle;
                E2eSupport.publish(topic, "c" + cycle + "-", RECORDS_PER_CYCLE);

                final Counter bean = new Counter();
                final ListenerMethodRegistry registry = new ListenerMethodRegistry();
                E2eSupport.registerListener(registry, group, topic, String.class, bean, "onMessage");

                final KafkaMessageConsumer consumer = virtual
                        ? virtualConsumer(registry, group, topic)
                        : osThreadConsumer(registry, group, topic);

                consumer.start();
                try {
                    E2eSupport.await("cycle " + cycle, TIMEOUT,
                            () -> bean.count.get() >= RECORDS_PER_CYCLE);
                } finally {
                    consumer.stop();
                }

                // Give the executors their shutdown window before sampling.
                E2eSupport.settle(Duration.ofMillis(1_500));
                final int threads = ManagementFactory.getThreadMXBean().getThreadCount();
                threadCounts.add(threads);
                System.out.printf("cycle=%d consumed=%d platformThreads=%d%n",
                        cycle, bean.count.get(), threads);
            }
        } finally {
            E2eSupport.deleteTopic(topic);
        }

        final int first = threadCounts.get(0);
        final int last = threadCounts.get(threadCounts.size() - 1);
        final int growth = last - first;
        System.out.printf("%s: threads after cycle 1 = %d, after cycle %d = %d (growth %+d)%n",
                virtual ? "virtual" : "OS", first, CYCLES, last, growth);

        // A leak shows up as monotonic growth roughly proportional to the cycle count. A few
        // threads of slack covers JIT/GC workers spinning up.
        assertTrue(growth <= CYCLES,
                "platform thread count grew by " + growth + " over " + CYCLES
                        + " consumer lifecycles: " + threadCounts);
    }

    private static KafkaMessageConsumer osThreadConsumer(ListenerMethodRegistry registry,
                                                        String group, String topic) {
        return KafkaConsumerBuilder.builder(registry)
                .properties(E2eSupport.consumerProperties())
                .groupId(group)
                .topics(topic)
                .offsetCommitStrategy(OffsetCommitStrategy.AT_LEAST_ONCE_BULK)
                .valueDeserializer(new E2eSupport.StringPayloadDeserializer())
                .autoPartitionPause(true)
                .invoker().unordered().singleExecutor()
                .coreThreadCount(4).maxThreadCount(8).keepAliveTime(1)
                .keepAliveTimeUnit(TimeUnit.SECONDS).queueCapacity(1_000)
                .and().and().and()
                .build();
    }

    private static KafkaMessageConsumer virtualConsumer(ListenerMethodRegistry registry,
                                                        String group, String topic) {
        return VirtualKafkaConsumerBuilder.builder(registry)
                .properties(E2eSupport.consumerProperties())
                .groupId(group)
                .topics(topic)
                .offsetCommitStrategy(OffsetCommitStrategy.AT_LEAST_ONCE_BULK)
                .valueDeserializer(new E2eSupport.StringPayloadDeserializer())
                .autoPartitionPause(true)
                .invoker().unordered().singleExecutor()
                .and().and().and()
                .build();
    }
}
