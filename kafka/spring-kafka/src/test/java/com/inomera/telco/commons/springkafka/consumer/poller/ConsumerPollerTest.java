package com.inomera.telco.commons.springkafka.consumer.poller;

import com.inomera.telco.commons.springkafka.consumer.KafkaConsumerProperties;
import com.inomera.telco.commons.springkafka.consumer.OffsetCommitStrategy;
import com.inomera.telco.commons.springkafka.consumer.invoker.BulkInvokerResult;
import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Poll-loop behaviour, driven through {@link MockConsumer}.
 */
class ConsumerPollerTest {

    private static final String TOPIC = "test-topic";
    private static final TopicPartition P0 = new TopicPartition(TOPIC, 0);
    private static final TopicPartition P1 = new TopicPartition(TOPIC, 1);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    // ------------------------------------------------------------- fixtures

    private static class TestableConsumerPoller extends DefaultConsumerPoller {
        private final Consumer<String, ?> delegate;

        TestableConsumerPoller(KafkaConsumerProperties properties, Consumer<String, ?> delegate) {
            super(properties, new StringDeserializer(), true);
            this.delegate = delegate;
        }

        @Override
        protected Consumer<String, ?> createConsumer() {
            return delegate;
        }
    }

    /**
     * Withholds the offsets it is told to, exactly as a {@code RETRY_FROM_BROKER} verdict would,
     * and records every verdict it was asked for so a test can tell "not yet reaped" apart from
     * "reaped and pinned".
     */
    private static final class WithholdingConsumerPoller extends TestableConsumerPoller {
        private final Set<Long> offsetsToWithhold;
        private final Set<Long> verdicts = ConcurrentHashMap.newKeySet();

        WithholdingConsumerPoller(KafkaConsumerProperties properties, Consumer<String, ?> delegate,
                                  Set<Long> offsetsToWithhold) {
            super(properties, delegate);
            this.offsetsToWithhold = offsetsToWithhold;
        }

        @Override
        protected boolean onResultCompleted(InvokerResult result) {
            final long offset = result.getRecord().offset();
            verdicts.add(offset);
            return !offsetsToWithhold.contains(offset);
        }
    }

    private static WithholdingConsumerPoller withholdingPoller(MockConsumer<String, Object> consumer,
                                                               Set<Long> offsetsToWithhold) {
        return new WithholdingConsumerPoller(properties(OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE),
                consumer, offsetsToWithhold);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class TestableBulkConsumerPoller extends BulkConsumerPoller {
        private final Consumer<String, ?> delegate;

        TestableBulkConsumerPoller(KafkaConsumerProperties properties, Consumer<String, ?> delegate) {
            super(properties, new StringDeserializer(), true);
            this.delegate = delegate;
        }

        @Override
        protected Consumer<String, ?> createConsumer() {
            return delegate;
        }
    }

    /**
     * Records consumer interactions in lock-free fields. Every {@link MockConsumer} method is
     * synchronized and the idle poll loop hammers that monitor, so a test thread that inspects
     * the consumer directly ends up measuring lock fairness rather than poller behaviour.
     */
    private static final class RecordingConsumer extends MockConsumer<String, Object> {
        private final AtomicReference<Set<TopicPartition>> lastAssignment = new AtomicReference<>(Set.of());
        private final AtomicReference<String> pauseCallerThread = new AtomicReference<>();
        private final Set<TopicPartition> pausedPartitions = ConcurrentHashMap.newKeySet();

        RecordingConsumer() {
            super("earliest");
        }

        @Override
        public ConsumerRecords<String, Object> poll(Duration timeout) {
            final ConsumerRecords<String, Object> records = super.poll(timeout);
            lastAssignment.set(Set.copyOf(super.assignment()));
            return records;
        }

        @Override
        public void pause(Collection<TopicPartition> partitions) {
            pauseCallerThread.set(Thread.currentThread().getName());
            pausedPartitions.addAll(partitions);
            super.pause(partitions);
        }
    }

    private static KafkaConsumerProperties properties(OffsetCommitStrategy strategy) {
        return new KafkaConsumerProperties("test-group", List.of(TOPIC), null, strategy, new Properties());
    }

    private static ConsumerRecord<String, Object> record(TopicPartition partition, long offset) {
        return new ConsumerRecord<>(partition.topic(), partition.partition(), offset, "key-" + offset,
                "payload-" + offset);
    }

    private static void await(String what, BooleanSupplier condition) {
        final Instant deadline = Instant.now().plus(TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("interrupted while waiting for " + what);
            }
        }
        fail("timed out waiting for " + what);
    }

    private static long committedOffset(MockConsumer<String, Object> consumer, TopicPartition partition) {
        final Map<TopicPartition, OffsetAndMetadata> committed = consumer.committed(Set.of(partition));
        final OffsetAndMetadata offsetAndMetadata = committed.get(partition);
        return offsetAndMetadata == null ? -1L : offsetAndMetadata.offset();
    }

    // ---------------------------------------------------------------- tests

    @Test
    @DisplayName("commits the highest completed offset for a partition")
    void commitsHighestCompletedOffset() {
        final MockConsumer<String, Object> consumer = new MockConsumer<>("earliest");
        consumer.updateBeginningOffsets(Map.of(P0, 0L));

        final TestableConsumerPoller poller =
                new TestableConsumerPoller(properties(OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE), consumer);
        poller.setConsumerRecordHandler(rec -> CompletableFuture.completedFuture(new InvokerResult(rec)));

        consumer.schedulePollTask(() -> {
            consumer.rebalance(List.of(P0));
            consumer.addRecord(record(P0, 0));
            consumer.addRecord(record(P0, 1));
            consumer.addRecord(record(P0, 2));
        });

        poller.start();
        try {
            // Three records consumed means the next offset to fetch is 3.
            await("offset 3 to be committed", () -> committedOffset(consumer, P0) == 3L);
        } finally {
            poller.stop();
        }
    }

    @Test
    @DisplayName("keeps in-flight state for partitions missing from an incremental assignment")
    void survivesIncrementalRebalance() {
        final MockConsumer<String, Object> consumer = new MockConsumer<>("earliest");
        consumer.updateBeginningOffsets(Map.of(P0, 0L, P1, 0L));

        final TestableConsumerPoller poller =
                new TestableConsumerPoller(properties(OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE), consumer);
        poller.setConsumerRecordHandler(rec -> CompletableFuture.completedFuture(new InvokerResult(rec)));

        consumer.schedulePollTask(() -> {
            consumer.rebalance(List.of(P0, P1));
            consumer.addRecord(record(P0, 0));
        });

        poller.start();
        try {
            await("first record to be committed", () -> committedOffset(consumer, P0) == 1L);

            // A cooperative/incremental assignment reports only the newly added partitions.
            // Wiping state here used to strand every partition we still owned.
            consumer.schedulePollTask(() -> {
                poller.onPartitionsAssigned(List.of(P1));
                consumer.addRecord(record(P0, 1));
            });

            await("partition 0 to keep making progress after the incremental assignment",
                    () -> committedOffset(consumer, P0) == 2L);
        } finally {
            poller.stop();
        }
    }

    @Test
    @DisplayName("bulk poller commits the highest offset of the batch, not the lowest")
    void bulkCommitsHighestOffsetOfBatch() {
        final MockConsumer<String, Object> consumer = new MockConsumer<>("earliest");
        consumer.updateBeginningOffsets(Map.of(P0, 0L));

        final TestableBulkConsumerPoller poller =
                new TestableBulkConsumerPoller(properties(OffsetCommitStrategy.AT_LEAST_ONCE_BULK), consumer);
        final BulkConsumerRecordHandler bulkHandler =
                records -> CompletableFuture.completedFuture(new BulkInvokerResult(records));
        poller.setConsumerRecordHandler(bulkHandler);

        consumer.schedulePollTask(() -> {
            consumer.rebalance(List.of(P0));
            for (long offset = 0; offset < 5; offset++) {
                consumer.addRecord(record(P0, offset));
            }
        });

        poller.start();
        try {
            await("offset 5 to be committed for the whole batch", () -> committedOffset(consumer, P0) == 5L);
        } finally {
            poller.stop();
        }
    }

    @Test
    @DisplayName("pause() issued from another thread is applied on the poller thread")
    void pauseFromAnotherThreadIsApplied() throws Exception {
        final RecordingConsumer consumer = new RecordingConsumer();
        consumer.updateBeginningOffsets(Map.of(P0, 0L, P1, 0L));

        final TestableConsumerPoller poller =
                new TestableConsumerPoller(properties(OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE), consumer);
        poller.setConsumerRecordHandler(rec -> new CompletableFuture<>());

        consumer.schedulePollTask(() -> consumer.rebalance(List.of(P0, P1)));

        poller.start();
        try {
            await("assignment", () -> consumer.lastAssignment.get().contains(P1));

            // Issue the pause from a thread that is definitely not the poller thread.
            final Thread caller = new Thread(() -> poller.pause(P1), "external-caller");
            caller.start();
            caller.join();

            await("partition 1 to be paused", () -> consumer.pausedPartitions.contains(P1));

            // The whole point of the command queue: the consumer is only ever touched by the
            // poller thread, never by the caller.
            final String pausedBy = consumer.pauseCallerThread.get();
            assertNotNull(pausedBy);
            assertTrue(pausedBy.contains("test-group"),
                    "pause must run on the poller thread but ran on " + pausedBy);
        } finally {
            poller.stop();
        }
    }

    @Test
    @DisplayName("a partition paused while nothing is in flight is resumed again")
    void idlePausedPartitionIsResumed() {
        final RecordingConsumer consumer = new RecordingConsumer();
        consumer.updateBeginningOffsets(Map.of(P0, 0L, P1, 0L));

        final TestableConsumerPoller poller =
                new TestableConsumerPoller(properties(OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE), consumer);
        poller.setConsumerRecordHandler(rec -> CompletableFuture.completedFuture(new InvokerResult(rec)));

        consumer.schedulePollTask(() -> consumer.rebalance(List.of(P0, P1)));

        poller.start();
        try {
            await("assignment", () -> consumer.lastAssignment.get().contains(P1));

            // pause() is asynchronous, so the rejection handler can land it after the partition's
            // last in-flight future was already reaped. Skipping idle partitions when resuming
            // left such a partition paused forever and consumption stopped dead.
            poller.pause(P1);
            await("pause to be applied", () -> consumer.pausedPartitions.contains(P1));

            await("the idle partition to be resumed", () -> !consumer.paused().contains(P1));
        } finally {
            poller.stop();
        }
    }

    @Test
    @DisplayName("a withheld offset stays withheld: later records must not be committed over it")
    void withheldOffsetIsNeverCommittedOver() {
        final MockConsumer<String, Object> consumer = new MockConsumer<>("earliest");
        consumer.updateBeginningOffsets(Map.of(P0, 0L));

        // Offset 1 is refused; 0 completed cleanly before it and 2 completes after it.
        final WithholdingConsumerPoller poller = withholdingPoller(consumer, Set.of(1L));
        poller.setConsumerRecordHandler(rec -> CompletableFuture.completedFuture(new InvokerResult(rec)));

        consumer.schedulePollTask(() -> {
            consumer.rebalance(List.of(P0));
            consumer.addRecord(record(P0, 0));
            consumer.addRecord(record(P0, 1));
            consumer.addRecord(record(P0, 2));
        });

        poller.start();
        try {
            // Everything before the withheld record is still committable: next fetch is offset 1.
            await("the prefix before the withheld record to be committed",
                    () -> committedOffset(consumer, P0) == 1L);

            // Give the loop plenty of cycles to get it wrong. Before 4.1.4 the break only held
            // the offset for one pass, so the next pass committed 3 and offset 1 was lost.
            sleepQuietly(500);
            assertEquals(1L, committedOffset(consumer, P0),
                    "offset 1 was withheld, so nothing after it may be committed");
        } finally {
            poller.stop();
        }
    }

    @Test
    @DisplayName("releasing a withheld offset lets the partition commit again")
    void releasingAWithheldOffsetResumesCommits() {
        final MockConsumer<String, Object> consumer = new MockConsumer<>("earliest");
        consumer.updateBeginningOffsets(Map.of(P0, 0L));

        final WithholdingConsumerPoller poller = withholdingPoller(consumer, Set.of(0L));
        poller.setConsumerRecordHandler(rec -> CompletableFuture.completedFuture(new InvokerResult(rec)));

        consumer.schedulePollTask(() -> {
            consumer.rebalance(List.of(P0));
            consumer.addRecord(record(P0, 0));
        });

        poller.start();
        try {
            await("the partition to be pinned", () -> poller.verdicts.contains(0L));
            sleepQuietly(200);
            assertEquals(-1L, committedOffset(consumer, P0), "the pinned record must not be committed");

            // This is what the in-memory retry consumer calls once a retry finally resolves.
            poller.releaseWithheldOffset(P0, 0L);
            consumer.schedulePollTask(() -> consumer.addRecord(record(P0, 1)));

            await("commits to resume after the release", () -> committedOffset(consumer, P0) == 2L);
        } finally {
            poller.stop();
        }
    }

    @Test
    @DisplayName("revoking a pinned partition does not commit over the withheld record")
    void revokeDoesNotCommitOverAWithheldRecord() throws Exception {
        final MockConsumer<String, Object> consumer = new MockConsumer<>("earliest");
        consumer.updateBeginningOffsets(Map.of(P0, 0L));

        final WithholdingConsumerPoller poller = withholdingPoller(consumer, Set.of(1L));
        poller.setConsumerRecordHandler(rec -> CompletableFuture.completedFuture(new InvokerResult(rec)));

        consumer.schedulePollTask(() -> {
            consumer.rebalance(List.of(P0));
            consumer.addRecord(record(P0, 0));
            consumer.addRecord(record(P0, 1));
            consumer.addRecord(record(P0, 2));
        });

        poller.start();
        try {
            await("the prefix before the withheld record to be committed",
                    () -> committedOffset(consumer, P0) == 1L);
            await("offset 2 to have been reaped", () -> poller.verdicts.contains(2L));

            // onPartitionsRevoked walked the in-flight queue and committed whatever had finished
            // without ever asking whether it succeeded, so a handover landing here committed
            // straight over the record we were told to hold back.
            final CountDownLatch revoked = new CountDownLatch(1);
            consumer.schedulePollTask(() -> {
                poller.onPartitionsRevoked(List.of(P0));
                revoked.countDown();
            });
            assertTrue(revoked.await(10, TimeUnit.SECONDS), "revoke callback never ran");

            assertEquals(1L, committedOffset(consumer, P0),
                    "the revoke path must not commit past a withheld offset");
        } finally {
            poller.stop();
        }
    }

    @Test
    @DisplayName("AT_MOST_ONCE_BULK survives a commit that times out, and loses no records")
    void atMostOnceSurvivesACommitTimeout() {
        // What a coordinator outage actually throws at the pre-dispatch commit. Only
        // CommitFailedException was caught, so this escaped dispatch(), unwound the poll loop
        // and killed the poller — on the one commit strategy that cannot fall back to
        // reprocessing, because its whole point is that records are never handled twice.
        final AtomicInteger commitCalls = new AtomicInteger();
        final AtomicBoolean commitsFail = new AtomicBoolean(true);
        final MockConsumer<String, Object> consumer = new MockConsumer<>("earliest") {
            @Override
            public synchronized void commitSync() {
                commitCalls.incrementAndGet();
                if (commitsFail.get()) {
                    throw new TimeoutException("coordinator unavailable");
                }
                super.commitSync();
            }
        };
        consumer.updateBeginningOffsets(Map.of(P0, 0L));

        final Set<Long> delivered = ConcurrentHashMap.newKeySet();
        final TestableConsumerPoller poller =
                new TestableConsumerPoller(properties(OffsetCommitStrategy.AT_MOST_ONCE_BULK), consumer);
        poller.setConsumerRecordHandler(rec -> {
            delivered.add(rec.offset());
            return CompletableFuture.completedFuture(new InvokerResult(rec));
        });

        consumer.schedulePollTask(() -> {
            consumer.rebalance(List.of(P0));
            consumer.addRecord(record(P0, 0));
            consumer.addRecord(record(P0, 1));
        });

        poller.start();
        try {
            await("the commit to be attempted and refused", () -> commitCalls.get() >= 1);

            // Nothing may have been handed to the listener: at-most-once dispatches only after
            // the commit is durable.
            assertTrue(delivered.isEmpty(), "records were dispatched despite the commit failing");
            // And the loop has to still be running. shouldRestart() is running && closed, i.e.
            // "the loop exited while it was still supposed to be going".
            assertFalse(poller.shouldRestart(), "the commit failure killed the poll loop");

            // Once the coordinator is back, the consumer must carry on rather than sit dead.
            commitsFail.set(false);
            consumer.schedulePollTask(() -> {
                consumer.addRecord(record(P0, 2));
                consumer.addRecord(record(P0, 3));
            });
            await("consumption to resume after the coordinator recovers",
                    () -> delivered.containsAll(Set.of(2L, 3L)));
            assertFalse(poller.shouldRestart(), "the poll loop exited after recovering");
        } finally {
            poller.stop();
        }
    }

    @Test
    @DisplayName("bulk poller rejects a single-record handler instead of silently ignoring it")
    void bulkPollerRejectsSingleRecordHandler() {
        final MockConsumer<String, Object> consumer = new MockConsumer<>("earliest");
        final TestableBulkConsumerPoller poller =
                new TestableBulkConsumerPoller(properties(OffsetCommitStrategy.AT_LEAST_ONCE_BULK), consumer);

        final ConsumerRecordHandler singleRecordHandler =
                rec -> CompletableFuture.completedFuture(new InvokerResult(rec));

        assertThrows(UnsupportedOperationException.class, () -> poller.setConsumerRecordHandler(singleRecordHandler));
    }

    @Test
    @DisplayName("group.protocol=consumer from configuration strips the settings the client rejects")
    void groupProtocolFromConfigurationIsNormalised() {
        // How it reaches the library in production: one properties blob out of application.yml
        // with group.protocol sitting next to the classic-only settings. Normalising this in the
        // builder was not enough — a poller built any other way still handed the client a config
        // it rejects — so it happens here, where properties become a KafkaConsumer.
        final Properties supplied = new Properties();
        supplied.setProperty("bootstrap.servers", "127.0.0.1:9092");
        supplied.setProperty("group.protocol", "consumer");
        supplied.setProperty("session.timeout.ms", "45000");
        supplied.setProperty("heartbeat.interval.ms", "3000");
        supplied.setProperty("partition.assignment.strategy",
                "org.apache.kafka.clients.consumer.CooperativeStickyAssignor");

        final KafkaConsumerProperties properties = new KafkaConsumerProperties(
                "test-group", List.of(TOPIC), null, OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE, supplied);
        final DefaultConsumerPoller poller =
                new DefaultConsumerPoller(properties, new StringDeserializer(), true);

        final Properties effective = poller.buildConsumerProperties();

        assertEquals("consumer", effective.getProperty("group.protocol"));
        assertFalse(effective.containsKey("session.timeout.ms"));
        assertFalse(effective.containsKey("heartbeat.interval.ms"));
        assertFalse(effective.containsKey("partition.assignment.strategy"));
        // The caller's own object is left alone; only the copy handed to the client is cleaned.
        assertTrue(supplied.containsKey("session.timeout.ms"));
    }

    @Test
    @DisplayName("stop() returns even when the poller was never started")
    void stopIsSafeWithoutStart() {
        final MockConsumer<String, Object> consumer = new MockConsumer<>("earliest");
        final TestableConsumerPoller poller =
                new TestableConsumerPoller(properties(OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE), consumer);

        poller.stop();
        assertEquals(false, poller.shouldRestart());
        assertTrue(true);
    }
}
