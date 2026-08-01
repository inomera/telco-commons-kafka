package com.inomera.telco.commons.springkafka.consumer.retry;

import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Retry semantics: real backoff, a hard capacity ceiling, dead-lettering on exhaustion, and a
 * {@code RETRY_FROM_BROKER} path that withholds the offset instead of killing the poll loop.
 */
class RetryBehaviourTest {

    private static final String TOPIC = "retry-topic";

    // ------------------------------------------------------------- fixtures

    @SuppressWarnings("unused")
    private static final class Listeners {
        @KafkaListener(groupId = "g", topics = TOPIC, retry = KafkaListener.RETRY.RETRY_FROM_BROKER)
        void fromBroker(Object message) {
        }

        @KafkaListener(groupId = "g", topics = TOPIC, retry = KafkaListener.RETRY.RETRY_IN_MEMORY_TASK,
                retryCount = 2, retryBackoffTime = 40L)
        void inMemory(Object message) {
        }

        @KafkaListener(groupId = "g", topics = TOPIC)
        void noRetry(Object message) {
        }
    }

    private static KafkaListener listener(String methodName) throws NoSuchMethodException {
        return Listeners.class.getDeclaredMethod(methodName, Object.class).getAnnotation(KafkaListener.class);
    }

    private static ConsumerRecord<String, Object> record(long offset) {
        return new ConsumerRecord<>(TOPIC, 0, offset, "k" + offset, "v" + offset);
    }

    private static InvokerResult failedResult(long offset, KafkaListener annotation) {
        final InvokerResult result = new InvokerResult(record(offset));
        // A non-null annotation is how the invoker signals "the listener threw".
        result.setKafkaListener(annotation);
        return result;
    }

    private static final class CapturingDeadLetterHandler implements DeadLetterHandler {
        private final List<Long> offsets = new ArrayList<>();
        private final CountDownLatch latch;

        private CapturingDeadLetterHandler(int expected) {
            this.latch = new CountDownLatch(expected);
        }

        @Override
        public synchronized void onExhausted(ConsumerRecord<String, ?> record, int attempts) {
            offsets.add(record.offset());
            latch.countDown();
        }
    }

    // ---------------------------------------------------------------- tests

    @Test
    @DisplayName("RETRY_FROM_BROKER withholds the offset instead of throwing into the poll loop")
    void retryFromBrokerWithholdsOffset() throws Exception {
        final DefaultRecordRetryer retryer = new DefaultRecordRetryer(null);

        // Must not throw: it used to raise RetriableCommitFailedException from inside the loop.
        final boolean committable = retryer.checkAndDecide(failedResult(7L, listener("fromBroker")));

        assertFalse(committable, "a RETRY_FROM_BROKER record's offset must not be committed");
    }

    @Test
    @DisplayName("a successful invocation is always committable")
    void successIsCommittable() {
        final DefaultRecordRetryer retryer = new DefaultRecordRetryer(null);
        // kafkaListener stays null when the listener completes normally.
        assertTrue(retryer.checkAndDecide(new InvokerResult(record(1L))));
    }

    @Test
    @DisplayName("backoff is actually waited out between attempts")
    void backoffIsApplied() throws Exception {
        final AtomicInteger attempts = new AtomicInteger();
        final CountDownLatch secondAttempt = new CountDownLatch(2);
        final KafkaListener annotation = listener("inMemory");

        final DefaultInMemoryRecordRetryConsumer retryConsumer = new DefaultInMemoryRecordRetryConsumer(
                rec -> {
                    attempts.incrementAndGet();
                    secondAttempt.countDown();
                    // Keep failing so it reschedules.
                    final InvokerResult result = new InvokerResult(rec);
                    result.setKafkaListener(annotation);
                    return CompletableFuture.completedFuture(result);
                },
                new RetryPolicy(5, Duration.ofMillis(120), Duration.ofSeconds(1), 2.0d,
                        Duration.ofSeconds(5), 100),
                new LoggingDeadLetterHandler());

        retryConsumer.start();
        try {
            final long startedAt = System.nanoTime();
            assertTrue(retryConsumer.offer(failedResult(1L, annotation), null));

            assertTrue(secondAttempt.await(10, TimeUnit.SECONDS), "expected at least two attempts");
            final long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;

            // Two attempts with a 120ms base and 50% jitter cannot complete instantly. The old
            // implementation re-queued immediately and burned every attempt in microseconds.
            assertTrue(elapsedMillis >= 100L,
                    "two attempts took only " + elapsedMillis + "ms, backoff was not applied");
        } finally {
            retryConsumer.stop();
        }
    }

    @Test
    @DisplayName("capacity is a hard ceiling and refusal is reported, not swallowed")
    void capacityIsEnforced() throws Exception {
        final KafkaListener annotation = listener("inMemory");
        final RetryPolicy tinyCapacity = new RetryPolicy(3, Duration.ofSeconds(30), Duration.ofSeconds(60),
                2.0d, Duration.ofSeconds(5), 2);

        // Never started, so nothing drains the queue and the capacity stays taken.
        final DefaultInMemoryRecordRetryConsumer retryConsumer = new DefaultInMemoryRecordRetryConsumer(
                rec -> CompletableFuture.completedFuture(new InvokerResult(rec)),
                tinyCapacity, new LoggingDeadLetterHandler());

        assertTrue(retryConsumer.offer(failedResult(1L, annotation), tinyCapacity));
        assertTrue(retryConsumer.offer(failedResult(2L, annotation), tinyCapacity));
        assertFalse(retryConsumer.offer(failedResult(3L, annotation), tinyCapacity),
                "third offer must be refused once capacity is exhausted");
        assertEquals(2, retryConsumer.pendingRetryCount());
    }

    @Test
    @DisplayName("a refused retry withholds the offset rather than dropping the record")
    void refusedRetryWithholdsOffset() throws Exception {
        final KafkaListener annotation = listener("inMemory");
        final RetryPolicy full = new RetryPolicy(3, Duration.ofSeconds(30), Duration.ofSeconds(60),
                2.0d, Duration.ofSeconds(5), 1);

        final DefaultInMemoryRecordRetryConsumer retryConsumer = new DefaultInMemoryRecordRetryConsumer(
                rec -> CompletableFuture.completedFuture(new InvokerResult(rec)),
                full, new LoggingDeadLetterHandler());
        final DefaultRecordRetryer retryer = new DefaultRecordRetryer(retryConsumer);

        // First one is accepted, so its offset may advance.
        assertTrue(retryer.checkAndDecide(failedResult(1L, annotation)));
        // Capacity is now full: the record must not be silently dropped.
        assertFalse(retryer.checkAndDecide(failedResult(2L, annotation)));
    }

    @Test
    @DisplayName("records are dead-lettered once attempts are exhausted")
    void exhaustedRecordsAreDeadLettered() throws Exception {
        final KafkaListener annotation = listener("inMemory");
        final CapturingDeadLetterHandler deadLetters = new CapturingDeadLetterHandler(1);

        final DefaultInMemoryRecordRetryConsumer retryConsumer = new DefaultInMemoryRecordRetryConsumer(
                rec -> {
                    final InvokerResult result = new InvokerResult(rec);
                    result.setKafkaListener(annotation);
                    return CompletableFuture.completedFuture(result);
                },
                new RetryPolicy(2, Duration.ofMillis(10), Duration.ofMillis(40), 2.0d,
                        Duration.ofSeconds(5), 10),
                deadLetters);

        retryConsumer.start();
        try {
            assertTrue(retryConsumer.offer(failedResult(42L, annotation), null));
            assertTrue(deadLetters.latch.await(10, TimeUnit.SECONDS), "expected a dead-letter callback");
            assertEquals(List.of(42L), deadLetters.offsets);
        } finally {
            retryConsumer.stop();
        }
    }
}
