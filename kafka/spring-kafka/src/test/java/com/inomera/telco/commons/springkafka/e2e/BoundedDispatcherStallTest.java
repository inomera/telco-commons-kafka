package com.inomera.telco.commons.springkafka.e2e;

import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import com.inomera.telco.commons.springkafka.builder.virtual.VirtualKafkaConsumerBuilder;
import com.inomera.telco.commons.springkafka.consumer.KafkaMessageConsumer;
import com.inomera.telco.commons.springkafka.consumer.OffsetCommitStrategy;
import com.inomera.telco.commons.springkafka.consumer.executor.virtual.BoundedVirtualExecutorStrategy;
import com.inomera.telco.commons.springkafka.consumer.invoker.ListenerMethodRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Diagnostic for the bounded virtual dispatcher: does a saturating backlog keep draining, or
 * does progress stop once the cap starts rejecting?
 */
class BoundedDispatcherStallTest {

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
    @DisplayName("bounded dispatcher keeps making progress under a saturating backlog")
    void keepsProgressing() {
        final int records = 3_000;
        final int cap = 64;
        final String topic = "stall-" + System.nanoTime();
        final String group = topic + "-group";

        E2eSupport.createTopic(topic, 6);
        E2eSupport.publish(topic, "m-", records);

        final Counter bean = new Counter();
        final ListenerMethodRegistry registry = new ListenerMethodRegistry();
        E2eSupport.registerListener(registry, group, topic, String.class, bean, "onMessage");

        final BoundedVirtualExecutorStrategy strategy = new BoundedVirtualExecutorStrategy(cap);
        final KafkaMessageConsumer consumer = VirtualKafkaConsumerBuilder.builder(registry)
                .properties(E2eSupport.consumerProperties())
                .groupId(group)
                .topics(topic)
                .offsetCommitStrategy(OffsetCommitStrategy.AT_MOST_ONCE_BULK)
                .valueDeserializer(new E2eSupport.StringPayloadDeserializer())
                .autoPartitionPause(true)
                .invoker()
                .unordered()
                .custom(strategy)
                .and()
                .build();

        consumer.start();
        try {
            final Instant deadline = Instant.now().plus(Duration.ofSeconds(60));
            int previous = -1;
            int stalledSamples = 0;
            while (Instant.now().isBefore(deadline)) {
                final int current = bean.count.get();
                System.out.printf("t=%s consumed=%d inFlight=%d%n",
                        Instant.now().toString().substring(11, 19), current, strategy.inFlight());
                if (current >= records) {
                    break;
                }
                stalledSamples = current == previous ? stalledSamples + 1 : 0;
                previous = current;
                try {
                    Thread.sleep(2_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.printf("FINAL consumed=%d / %d, inFlight=%d, stalledSamples=%d%n",
                    bean.count.get(), records, strategy.inFlight(), stalledSamples);
            assertTrue(bean.count.get() >= records,
                    "only " + bean.count.get() + "/" + records + " consumed; dispatcher stalled");
        } finally {
            consumer.stop();
            E2eSupport.deleteTopic(topic);
        }
    }
}
