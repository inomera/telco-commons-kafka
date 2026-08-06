package com.inomera.telco.commons.springkafka.consumer.poller;

import com.inomera.telco.commons.lang.PropertyUtils;
import com.inomera.telco.commons.lang.thread.FutureUtils;
import com.inomera.telco.commons.lang.thread.IncrementalNamingThreadFactory;
import com.inomera.telco.commons.springkafka.consumer.KafkaConsumerProperties;
import com.inomera.telco.commons.springkafka.util.InterruptUtils;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Properties;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared poll-loop machinery for the single-record and bulk pollers.
 *
 * <h2>Threading</h2>
 * A {@link Consumer} is not safe for multi-threaded access: {@code wakeup()} is the only
 * method that may be called from another thread. Everything else in this class runs on the
 * single poller thread, which also means the in-flight bookkeeping needs no locking. Callers
 * that live on other threads (for example a rejection handler) go through
 * {@link #pause(TopicPartition)}, which enqueues a command and wakes the loop up rather than
 * touching the consumer directly.
 *
 * <h2>Offsets</h2>
 * At-most-once commits happen before dispatch and stay synchronous, because the guarantee
 * depends on the commit being durable first. At-least-once commits are accumulated and
 * flushed once per poll cycle with {@code commitAsync}, which removes the per-record broker
 * round trip without weakening the guarantee.
 *
 * @param <R> invocation result type produced for a dispatched unit of work
 * @author Serdar Kuzucu
 * @author Turgay Can
 */
public abstract class AbstractConsumerPoller<R> implements ConsumerPoller, Runnable, ConsumerRebalanceListener {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractConsumerPoller.class);

    private static final long MIN_POLL_WAIT_MS = 30L;
    private static final long MAX_POLL_WAIT_MS = 3_000L;
    private static final long STOP_TIMEOUT_SECONDS = 30L;
    private static final long REFUSED_COMMIT_BACKOFF_STEP_MS = 50L;

    /** A unit of work to run against the consumer on the poller thread. */
    @FunctionalInterface
    protected interface ConsumerCommand {
        void execute(Consumer<String, ?> consumer);
    }

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(true);
    private final Queue<ConsumerCommand> commands = new ConcurrentLinkedQueue<>();

    /** Poller-thread confined. Never touched from another thread, so no locking. */
    private final Map<TopicPartition, ArrayDeque<Future<R>>> inFlight = new HashMap<>();
    /** Poller-thread confined. Offsets waiting to be flushed by the next commitAsync. */
    private final Map<TopicPartition, OffsetAndMetadata> pendingOffsets = new HashMap<>();
    /**
     * Poller-thread confined. Offsets whose verdict was "withhold" and that have not been
     * resolved since. While a partition has an entry here nothing on it may be committed.
     */
    private final Map<TopicPartition, NavigableSet<Long>> withheldOffsets = new HashMap<>();
    /**
     * Poller-thread confined. Where to resume each partition from if the current poll cycle has
     * to be abandoned because an at-most-once pre-dispatch commit was refused.
     */
    private final Map<TopicPartition, Long> rewindTargets = new HashMap<>();

    private final KafkaConsumerProperties kafkaConsumerProperties;
    private final Deserializer<?> valueDeserializer;
    private final boolean autoPartitionPause;

    private ThreadFactory consumerThreadFactory;
    private ExecutorService executorService;
    private Consumer<String, ?> consumer;
    private CountDownLatch terminationLatch;
    private boolean commitInFlight;
    private int consecutiveRefusedCommits;

    protected AbstractConsumerPoller(KafkaConsumerProperties kafkaConsumerProperties,
                                     Deserializer<?> valueDeserializer,
                                     boolean autoPartitionPause) {
        this.kafkaConsumerProperties = kafkaConsumerProperties;
        this.valueDeserializer = valueDeserializer;
        this.autoPartitionPause = autoPartitionPause;
    }

    // ------------------------------------------------------------------ hooks

    /**
     * Hands the polled records to the invoker. Implementations call
     * {@link #track(TopicPartition, Future)} for each dispatched unit and add every partition
     * they dispatched from to {@code toPause}.
     *
     * @return {@code false} to abandon this poll cycle (an at-most-once commit failed)
     */
    protected abstract boolean dispatch(ConsumerRecords<String, ?> records, Collection<TopicPartition> toPause);

    /** Highest record offset covered by a completed result. */
    protected abstract long highestOffset(R result);

    /**
     * Called once for each completed result before its offset is committed. This is where the
     * retry policy runs, so it has side effects and must never be invoked twice for the same
     * result.
     *
     * @return {@code false} to withhold this offset (and everything after it on the same
     *         partition) so the broker re-delivers the record later
     */
    protected abstract boolean onResultCompleted(R result);

    /**
     * Side-effect-free counterpart of {@link #onResultCompleted}: did the listener succeed?
     *
     * <p>Used on the rebalance path, where we still want to commit the work that finished but
     * must not run the retry policy for a partition we are about to hand over.
     */
    protected abstract boolean isCommittable(R result);

    /** Guards {@link #dispatch} preconditions, mirroring the old {@code Assert} in {@code start()}. */
    protected abstract void assertReadyToStart();

    // ------------------------------------------------------------- lifecycle

    @Override
    public void start() {
        assertReadyToStart();
        LOG.info("Starting consumer. group={}", kafkaConsumerProperties.getGroupId());

        shutdownExecutorIfNotNull();
        closeConsumerSilently();

        inFlight.clear();
        pendingOffsets.clear();
        withheldOffsets.clear();
        rewindTargets.clear();
        commands.clear();
        commitInFlight = false;
        consecutiveRefusedCommits = 0;

        this.consumer = createConsumer();
        this.terminationLatch = new CountDownLatch(1);

        // The Kafka consumer thread must stay an OS thread: the poll loop is CPU bound and
        // the consumer is bound to the thread that created it.
        final ThreadFactory threadFactory = this.consumerThreadFactory == null
                ? new IncrementalNamingThreadFactory(kafkaConsumerProperties.getGroupId())
                : this.consumerThreadFactory;
        this.executorService = new ThreadPoolExecutor(0, 1, 0, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(), threadFactory);

        // Both flags must be visible before the runnable can observe them, otherwise the loop
        // can start, read running==false and exit immediately, leaving a silently dead consumer.
        closed.set(false);
        running.set(true);
        executorService.submit(this);
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        final Consumer<String, ?> current = this.consumer;
        if (current != null) {
            current.wakeup();
        }

        final CountDownLatch latch = this.terminationLatch;
        if (latch != null) {
            try {
                if (!latch.await(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    LOG.warn("Consumer {} did not stop within {}s; shutting the poller thread down anyway.",
                            kafkaConsumerProperties.getGroupId(), STOP_TIMEOUT_SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        shutdownExecutorIfNotNull();
    }

    @Override
    public boolean shouldRestart() {
        return this.running.get() && this.closed.get();
    }

    /** Overridable so tests can substitute a {@code MockConsumer}. */
    protected Consumer<String, ?> createConsumer() {
        return new KafkaConsumer<>(buildConsumerProperties(), new StringDeserializer(), valueDeserializer);
    }

    // ------------------------------------------------------------- poll loop

    @Override
    public void run() {
        try {
            subscribe();

            long pollWaitMs = MIN_POLL_WAIT_MS;
            while (running.get()) {
                drainCommands();

                final ConsumerRecords<String, ?> records;
                try {
                    records = consumer.poll(Duration.ofMillis(pollWaitMs));
                } catch (WakeupException e) {
                    // wakeup() is how other threads hand us work; it is only fatal while stopping.
                    if (!running.get()) {
                        break;
                    }
                    continue;
                }

                if (records.isEmpty()) {
                    pollWaitMs = Math.min(pollWaitMs * 2, MAX_POLL_WAIT_MS);
                } else {
                    pollWaitMs = MIN_POLL_WAIT_MS;
                    final Collection<TopicPartition> toPause = new HashSet<>();
                    resetRewindTargets(records);
                    if (!dispatch(records, toPause)) {
                        rewindAbandonedCycle();
                        continue;
                    }
                    consecutiveRefusedCommits = 0;
                    if (autoPartitionPause && !toPause.isEmpty()) {
                        LOG.debug("PAUSED-> {}", toPause);
                        consumer.pause(toPause);
                    }
                }

                reapCompleted();
                flushPendingOffsets();
            }
        } catch (Exception e) {
            LOG.error("Exception occurred in the poll loop of {}: {}",
                    kafkaConsumerProperties.getGroupId(), e.getMessage(), e);
            InterruptUtils.interruptIfInterruptedException(e);
        } finally {
            commitPendingSync();
            closeConsumerSilently();
            closed.set(true);
            if (terminationLatch != null) {
                terminationLatch.countDown();
            }
        }
    }

    private void subscribe() {
        if (kafkaConsumerProperties.hasPatternBasedTopic()) {
            consumer.subscribe(kafkaConsumerProperties.getTopicPattern(), this);
        } else {
            consumer.subscribe(kafkaConsumerProperties.getTopics(), this);
        }
    }

    private void drainCommands() {
        ConsumerCommand command;
        while ((command = commands.poll()) != null) {
            try {
                command.execute(consumer);
            } catch (Exception e) {
                LOG.error("Consumer command failed: {}", e.getMessage(), e);
                InterruptUtils.interruptIfInterruptedException(e);
            }
        }
    }

    /**
     * Collects the completed prefix of every partition, feeds each result to the retry hook and
     * queues its offset. A partition is resumed once nothing is left in flight for it.
     *
     * <p>Note an idle partition still has to be considered for resuming. {@link #pause} is
     * asynchronous, so the rejection handler can pause a partition <em>after</em> its last
     * in-flight future has already been reaped; skipping empty partitions here left such a
     * partition paused forever and consumption stopped dead.
     *
     * <p>A withheld offset stays withheld. Breaking out of the loop only held the offset back
     * for the current pass: the withheld future had already been removed, so the next pass
     * started at the record after it, found it committable and committed straight over the top
     * of the record we were told not to commit. That is silent message loss, and it is exactly
     * the case {@code RETRY_FROM_BROKER} exists to prevent. The partition is now pinned instead,
     * and stays pinned until every withheld offset on it resolves — which for
     * {@code RETRY_FROM_BROKER} means never, so the records are re-delivered on the next
     * restart or rebalance.
     */
    private void reapCompleted() {
        if (inFlight.isEmpty()) {
            return;
        }

        final Collection<TopicPartition> idle = new HashSet<>();

        for (Map.Entry<TopicPartition, ArrayDeque<Future<R>>> entry : inFlight.entrySet()) {
            final TopicPartition partition = entry.getKey();
            final ArrayDeque<Future<R>> queue = entry.getValue();
            if (queue.isEmpty()) {
                idle.add(partition);
                continue;
            }

            // Records keep flowing while a partition is pinned; only its offsets are frozen.
            boolean pinned = withheldOffsets.containsKey(partition);
            long highestCompleted = -1L;
            // Stop at the first incomplete future: offsets must stay in order.
            for (Iterator<Future<R>> it = queue.iterator(); it.hasNext(); ) {
                final Future<R> future = it.next();
                if (!future.isDone()) {
                    break;
                }
                it.remove();
                final R result = FutureUtils.getUnchecked(future);
                if (result == null) {
                    continue;
                }
                if (!onResultCompleted(result)) {
                    pinned = true;
                    withhold(partition, highestOffset(result));
                    continue;
                }
                if (!pinned) {
                    highestCompleted = Math.max(highestCompleted, highestOffset(result));
                }
            }

            if (highestCompleted >= 0 && kafkaConsumerProperties.isAtLeastOnce()) {
                markCommittable(partition, highestCompleted);
            }
            if (queue.isEmpty()) {
                idle.add(partition);
            }
        }

        if (idle.isEmpty()) {
            return;
        }

        // Resume only what is actually paused, so an idle partition can never be left behind and
        // we do not issue a resume for partitions that were never paused.
        final Collection<TopicPartition> toResume = new HashSet<>(idle);
        toResume.retainAll(consumer.paused());
        if (!toResume.isEmpty()) {
            consumer.resume(toResume);
            LOG.debug("RESUMED-> {}", toResume);
        }
    }

    // ------------------------------------------------- abandoned poll cycles

    /**
     * Marks every partition in the batch as "resume from the first record we just polled".
     * Subclasses move the mark forward with {@link #advanceRewindTarget} as they commit and
     * dispatch, so an abandoned cycle rewinds to the first record that was <em>not</em> handled.
     */
    private void resetRewindTargets(ConsumerRecords<String, ?> records) {
        rewindTargets.clear();
        for (TopicPartition partition : records.partitions()) {
            final List<? extends ConsumerRecord<String, ?>> partitionRecords = records.records(partition);
            if (!partitionRecords.isEmpty()) {
                rewindTargets.put(partition, partitionRecords.get(0).offset());
            }
        }
    }

    /**
     * Reports that everything before {@code nextOffset} on this partition has been committed and
     * dispatched, so an abandoned cycle must not rewind past it and hand those records to the
     * listener a second time.
     */
    protected void advanceRewindTarget(TopicPartition partition, long nextOffset) {
        rewindTargets.merge(partition, nextOffset, Math::max);
    }

    /**
     * Puts the fetch position back after an at-most-once pre-dispatch commit was refused.
     *
     * <p>{@code poll()} has already advanced the position past everything it returned, so simply
     * abandoning the cycle threw the batch away: the records were neither committed nor
     * dispatched, and nothing ever re-read them. That is silent loss on the one strategy whose
     * users chose it because they cannot reprocess a record. Seeking back makes the next poll
     * return the same records so the commit can be attempted again — and because the target only
     * ever moves forward as records are committed and dispatched, nothing is delivered twice.
     */
    private void rewindAbandonedCycle() {
        final Collection<TopicPartition> assigned = consumer.assignment();
        for (Map.Entry<TopicPartition, Long> entry : rewindTargets.entrySet()) {
            if (!assigned.contains(entry.getKey())) {
                // Revoked in the meantime; the new owner resumes from the committed offset.
                continue;
            }
            try {
                consumer.seek(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                LOG.error("Could not rewind {} to {} after a refused commit; those records will be "
                        + "skipped: {}", entry.getKey(), entry.getValue(), e.getMessage(), e);
                InterruptUtils.interruptIfInterruptedException(e);
            }
        }
        backOffAfterRefusedCommit();
    }

    /**
     * Keeps a coordinator that is refusing commits from being hammered. Without it the loop spins
     * poll → commit → seek as fast as the network allows.
     */
    private void backOffAfterRefusedCommit() {
        final long backoffMillis = Math.min(REFUSED_COMMIT_BACKOFF_STEP_MS * ++consecutiveRefusedCommits,
                MAX_POLL_WAIT_MS);
        if (consecutiveRefusedCommits == 1 || consecutiveRefusedCommits % 20 == 0) {
            LOG.warn("Pre-dispatch commit refused {} time(s) in a row for {}; rewound and backing off {}ms.",
                    consecutiveRefusedCommits, kafkaConsumerProperties.getGroupId(), backoffMillis);
        }
        try {
            Thread.sleep(backoffMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // --------------------------------------------------------------- offsets

    /**
     * Records the highest committable offset for a partition; flushed later in the cycle.
     *
     * <p>Anything at or after the earliest unresolved offset on the partition is dropped:
     * committing it would tell the broker that the withheld record was consumed. Offsets
     * strictly before it are still fine and worth keeping — they are the work that genuinely
     * completed before things went wrong.
     */
    protected void markCommittable(TopicPartition partition, long offset) {
        final NavigableSet<Long> withheld = withheldOffsets.get(partition);
        if (withheld != null && !withheld.isEmpty() && offset >= withheld.first()) {
            return;
        }
        pendingOffsets.merge(partition, new OffsetAndMetadata(offset + 1),
                (current, candidate) -> candidate.offset() > current.offset() ? candidate : current);
    }

    /** Pins a partition's committed offset at {@code offset} until that record resolves. */
    private void withhold(TopicPartition partition, long offset) {
        final NavigableSet<Long> offsets = withheldOffsets.computeIfAbsent(partition, key -> new TreeSet<>());
        if (offsets.add(offset) && offsets.size() == 1) {
            LOG.warn("Withholding offsets for {} from {} onward for group {}. Consumer lag on this "
                            + "partition will not decrease until the record resolves or the consumer restarts.",
                    partition, offset, kafkaConsumerProperties.getGroupId());
        }
    }

    /**
     * Reports that a previously withheld record finally resolved, so its partition may commit
     * again. Callable from any thread: the release runs on the poller thread.
     *
     * <p>"Resolved" means terminal, not successful — a dead-lettered record is resolved too,
     * because the dead-letter handler has taken ownership of it.
     */
    public void releaseWithheldOffset(TopicPartition partition, long offset) {
        commands.offer(consumer -> {
            final NavigableSet<Long> offsets = withheldOffsets.get(partition);
            if (offsets == null || !offsets.remove(offset)) {
                return;
            }
            if (offsets.isEmpty()) {
                withheldOffsets.remove(partition);
                LOG.info("Offset {} on {} resolved; commits resume for this partition.", offset, partition);
            }
        });
        final Consumer<String, ?> current = this.consumer;
        if (current != null) {
            current.wakeup();
        }
    }

    /** Convenience overload for callers that only hold the record. */
    public void releaseWithheldOffset(ConsumerRecord<String, ?> record) {
        releaseWithheldOffset(new TopicPartition(record.topic(), record.partition()), record.offset());
    }

    private void flushPendingOffsets() {
        if (pendingOffsets.isEmpty() || commitInFlight) {
            return;
        }

        final Map<TopicPartition, OffsetAndMetadata> batch = Map.copyOf(pendingOffsets);
        pendingOffsets.clear();
        commitInFlight = true;

        // The callback is invoked on this same poller thread from within poll().
        consumer.commitAsync(batch, (offsets, exception) -> {
            commitInFlight = false;
            if (exception == null) {
                return;
            }
            if (exception instanceof RetriableException) {
                LOG.warn("Retriable commit failure for {}, re-queueing {} offsets.",
                        kafkaConsumerProperties.getClientId(), offsets.size(), exception);
                offsets.forEach((tp, offset) -> pendingOffsets.merge(tp, offset,
                        (current, candidate) -> candidate.offset() > current.offset() ? candidate : current));
            } else {
                LOG.error("Offset commit failed for {}. offsets={}",
                        kafkaConsumerProperties.getClientId(), offsets, exception);
            }
        });
    }

    private void commitPendingSync() {
        if (pendingOffsets.isEmpty() || consumer == null) {
            return;
        }
        final Map<TopicPartition, OffsetAndMetadata> batch = Map.copyOf(pendingOffsets);
        try {
            consumer.commitSync(batch);
            pendingOffsets.clear();
        } catch (WakeupException e) {
            // stop() wakes the consumer up before we get here, so the shutdown commit is the
            // most likely victim. The flag is consumed by the throw, so one retry is enough.
            try {
                consumer.commitSync(batch);
                pendingOffsets.clear();
            } catch (Exception retryFailure) {
                LOG.error("Final offset commit failed for {} after a wakeup: {}",
                        kafkaConsumerProperties.getClientId(), retryFailure.getMessage(), retryFailure);
                InterruptUtils.interruptIfInterruptedException(retryFailure);
            }
        } catch (Exception e) {
            LOG.error("Final offset commit failed for {}: {}",
                    kafkaConsumerProperties.getClientId(), e.getMessage(), e);
            InterruptUtils.interruptIfInterruptedException(e);
        }
    }

    /**
     * Synchronous commit used by the at-most-once strategies, which must commit before dispatch.
     *
     * <p>{@code wakeup()} interrupts <em>any</em> blocking consumer call, not just {@code poll},
     * so a {@link #pause} issued from another thread can land here. Only
     * {@code CommitFailedException} used to be caught, which let the {@code WakeupException}
     * unwind the poll loop and kill the poller.
     */
    protected boolean commitSyncBeforeProcessing(TopicPartition partition, long offset) {
        final Map<TopicPartition, OffsetAndMetadata> batch =
                Map.of(partition, new OffsetAndMetadata(offset + 1));
        try {
            consumer.commitSync(batch);
            return true;
        } catch (WakeupException e) {
            if (!retryAfterWakeup(() -> consumer.commitSync(batch))) {
                LOG.debug("Commit for {}@{} abandoned after a wakeup.", partition, offset);
                return false;
            }
            return true;
        } catch (CommitFailedException e) {
            LOG.warn("Offset commit refused for {}. partition={}, offset={}: {}",
                    kafkaConsumerProperties.getClientId(), partition, offset, e.getMessage());
            return false;
        } catch (Exception e) {
            // Anything else — a TimeoutException while the coordinator is unreachable is the
            // common one — used to escape dispatch(), unwind the poll loop and kill the poller.
            LOG.error("Offset commit failed for {}. partition={}, offset={}: {}",
                    kafkaConsumerProperties.getClientId(), partition, offset, e.getMessage(), e);
            InterruptUtils.interruptIfInterruptedException(e);
            return false;
        }
    }

    /** Synchronous bulk commit used by {@code AT_MOST_ONCE_BULK}. See {@link #commitSyncBeforeProcessing}. */
    protected boolean commitAllSyncBeforeProcessing() {
        try {
            consumer.commitSync();
            return true;
        } catch (WakeupException e) {
            if (!retryAfterWakeup(() -> consumer.commitSync())) {
                LOG.debug("Bulk commit abandoned after a wakeup.");
                return false;
            }
            return true;
        } catch (CommitFailedException e) {
            // WARN, not INFO with a stack trace: this refuses a whole poll batch. The usual cause
            // under group.protocol=consumer is STALE_MEMBER_EPOCH, which the client does not
            // retry for us — StaleMemberEpochException is an ApiException, not a
            // RetriableException, so commitSync gives up on the first response.
            LOG.warn("Committing last offsets refused for {}: {}. Rewinding and retrying.",
                    kafkaConsumerProperties.getClientId(), e.getMessage());
            return false;
        } catch (Exception e) {
            LOG.error("Committing last offsets failed for {}: {}",
                    kafkaConsumerProperties.getClientId(), e.getMessage(), e);
            InterruptUtils.interruptIfInterruptedException(e);
            return false;
        }
    }

    /**
     * Re-runs a commit that a {@code wakeup()} interrupted.
     *
     * <p>An at-most-once commit that gives up takes the rest of the poll batch with it: those
     * records were already fetched, so the consumer's position has moved past them and nothing
     * re-delivers them until a rebalance. The wakeup flag is consumed by the throw, so a single
     * retry gets the commit through, and the command that triggered the wakeup is still drained
     * at the top of the next iteration.
     *
     * <p>While stopping there is nothing to salvage — the batch is not going to be dispatched
     * anyway — so the retry is skipped and shutdown is not delayed by a commit round trip.
     *
     * @return {@code true} if the retry committed
     */
    private boolean retryAfterWakeup(Runnable commit) {
        if (!running.get()) {
            return false;
        }
        try {
            commit.run();
            return true;
        } catch (Exception e) {
            LOG.error("Offset commit for {} failed on the retry after a wakeup: {}",
                    kafkaConsumerProperties.getClientId(), e.getMessage(), e);
            InterruptUtils.interruptIfInterruptedException(e);
            return false;
        }
    }

    // ------------------------------------------------------------- rebalance

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        // Never clear: with cooperative/incremental assignment this callback only carries the
        // newly added partitions, and dropping the rest would strand their in-flight work.
        for (TopicPartition partition : partitions) {
            inFlight.computeIfAbsent(partition, key -> new ArrayDeque<>());
        }
        LOG.info("ASSIGNED-> {}", partitions);
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitionsRevoked) {
        if (kafkaConsumerProperties.isAtLeastOnce()) {
            for (TopicPartition partition : partitionsRevoked) {
                final ArrayDeque<Future<R>> queue = inFlight.get(partition);
                if (queue == null || withheldOffsets.containsKey(partition)) {
                    // Pinned partitions commit nothing, here least of all.
                    continue;
                }
                for (Future<R> future : queue) {
                    if (!future.isDone()) {
                        break;
                    }
                    final R result = FutureUtils.getUnchecked(future);
                    if (result == null) {
                        continue;
                    }
                    // isCommittable, not onResultCompleted: this path used to commit failed
                    // records because it never asked whether the listener actually succeeded,
                    // so a rebalance landing on a failure committed straight over it. Running
                    // the full retry hook here is not an option either — it would schedule a
                    // retry for a partition we are in the middle of handing over.
                    if (!isCommittable(result)) {
                        break;
                    }
                    markCommittable(partition, highestOffset(result));
                }
            }
            commitPendingSync();
        }
        partitionsRevoked.forEach(partition -> {
            inFlight.remove(partition);
            withheldOffsets.remove(partition);
        });
        LOG.info("REVOKED-> {}", partitionsRevoked);
    }

    @Override
    public void onPartitionsLost(Collection<TopicPartition> partitionsLost) {
        // The partitions are already owned by someone else; committing would fail or, worse,
        // overwrite the new owner's progress.
        partitionsLost.forEach(partition -> {
            inFlight.remove(partition);
            pendingOffsets.remove(partition);
            withheldOffsets.remove(partition);
        });
        LOG.warn("LOST-> {}", partitionsLost);
    }

    // ----------------------------------------------------------------- misc

    @Override
    public void pause(TopicPartition topicPartition) {
        // Callable from any thread: the actual pause runs on the poller thread.
        commands.offer(consumer -> consumer.pause(List.of(topicPartition)));
        final Consumer<String, ?> current = this.consumer;
        if (current != null) {
            current.wakeup();
        }
    }

    /** Registers a dispatched unit of work so its offset can be committed once it completes. */
    protected void track(TopicPartition partition, Future<R> future) {
        inFlight.computeIfAbsent(partition, key -> new ArrayDeque<>()).add(future);
    }

    protected Properties buildConsumerProperties() {
        final Properties props = PropertyUtils.copyProperties(kafkaConsumerProperties.getKafkaConsumerProperties());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConsumerProperties.getGroupId());
        if (kafkaConsumerProperties.hasPatternBasedTopic()) {
            /*
             * We are decreasing the default metadata load period, which is 5
             * minutes, to 30s so that pattern based subscriptions take affect
             * shorter.
             */
            props.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, "30000");
        }
        props.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, kafkaConsumerProperties.getClientId());
        return PropertyUtils.overrideWithSystemArguments(props);
    }

    private void shutdownExecutorIfNotNull() {
        if (this.executorService != null) {
            this.executorService.shutdownNow();
            this.executorService = null;
        }
    }

    private void closeConsumerSilently() {
        if (consumer == null) {
            return;
        }
        try {
            consumer.close();
        } catch (Exception e) {
            LOG.trace("Exception in closeConsumerSilently: {}", e.getMessage(), e);
        }
    }

    public KafkaConsumerProperties getKafkaConsumerProperties() {
        return kafkaConsumerProperties;
    }

    public Deserializer<?> getValueDeserializer() {
        return valueDeserializer;
    }

    public boolean isAutoPartitionPause() {
        return autoPartitionPause;
    }

    public void setConsumerThreadFactory(ThreadFactory consumerThreadFactory) {
        this.consumerThreadFactory = consumerThreadFactory;
    }

    protected boolean isRunning() {
        return running.get();
    }
}
