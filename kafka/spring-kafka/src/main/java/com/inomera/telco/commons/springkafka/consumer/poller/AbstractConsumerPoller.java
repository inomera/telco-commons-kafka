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
import java.util.Properties;
import java.util.Queue;
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

    private final KafkaConsumerProperties kafkaConsumerProperties;
    private final Deserializer<?> valueDeserializer;
    private final boolean autoPartitionPause;

    private ThreadFactory consumerThreadFactory;
    private ExecutorService executorService;
    private Consumer<String, ?> consumer;
    private CountDownLatch terminationLatch;
    private boolean commitInFlight;

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
     * Called for each completed result before its offset is committed.
     *
     * @return {@code false} to withhold this offset (and everything after it on the same
     *         partition) so the broker re-delivers the record later
     */
    protected abstract boolean onResultCompleted(R result);

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
        commands.clear();
        commitInFlight = false;

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
                    if (!dispatch(records, toPause)) {
                        continue;
                    }
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
     */
    private void reapCompleted() {
        if (inFlight.isEmpty()) {
            return;
        }

        final Collection<TopicPartition> toResume = new HashSet<>();

        for (Map.Entry<TopicPartition, ArrayDeque<Future<R>>> entry : inFlight.entrySet()) {
            final ArrayDeque<Future<R>> queue = entry.getValue();
            if (queue.isEmpty()) {
                continue;
            }

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
                    // Retry policy asked us to hold this offset back. Nothing after it on this
                    // partition may be committed either, or we would skip over it.
                    break;
                }
                highestCompleted = Math.max(highestCompleted, highestOffset(result));
            }

            if (highestCompleted >= 0 && kafkaConsumerProperties.isAtLeastOnce()) {
                markCommittable(entry.getKey(), highestCompleted);
            }
            if (queue.isEmpty()) {
                toResume.add(entry.getKey());
            }
        }

        if (autoPartitionPause && !toResume.isEmpty()) {
            consumer.resume(toResume);
            LOG.debug("RESUMED-> {}", toResume);
        }
    }

    // --------------------------------------------------------------- offsets

    /** Records the highest committable offset for a partition; flushed later in the cycle. */
    protected void markCommittable(TopicPartition partition, long offset) {
        pendingOffsets.merge(partition, new OffsetAndMetadata(offset + 1),
                (current, candidate) -> candidate.offset() > current.offset() ? candidate : current);
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
        try {
            consumer.commitSync(Map.copyOf(pendingOffsets));
            pendingOffsets.clear();
        } catch (Exception e) {
            LOG.error("Final offset commit failed for {}: {}",
                    kafkaConsumerProperties.getClientId(), e.getMessage(), e);
            InterruptUtils.interruptIfInterruptedException(e);
        }
    }

    /** Synchronous commit used by the at-most-once strategies, which must commit before dispatch. */
    protected boolean commitSyncBeforeProcessing(TopicPartition partition, long offset) {
        try {
            consumer.commitSync(Map.of(partition, new OffsetAndMetadata(offset + 1)));
            return true;
        } catch (CommitFailedException e) {
            LOG.error("Offset commit failed for {}. partition={}, offset={}",
                    kafkaConsumerProperties.getClientId(), partition, offset, e);
            return false;
        }
    }

    /** Synchronous bulk commit used by {@code AT_MOST_ONCE_BULK}. */
    protected boolean commitAllSyncBeforeProcessing() {
        try {
            consumer.commitSync();
            return true;
        } catch (CommitFailedException e) {
            LOG.info("Committing last offsets failed for {}.", kafkaConsumerProperties.getClientId(), e);
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
                if (queue == null) {
                    continue;
                }
                for (Future<R> future : queue) {
                    if (!future.isDone()) {
                        break;
                    }
                    final R result = FutureUtils.getUnchecked(future);
                    if (result != null) {
                        markCommittable(partition, highestOffset(result));
                    }
                }
            }
            commitPendingSync();
        }
        partitionsRevoked.forEach(inFlight::remove);
        LOG.info("REVOKED-> {}", partitionsRevoked);
    }

    @Override
    public void onPartitionsLost(Collection<TopicPartition> partitionsLost) {
        // The partitions are already owned by someone else; committing would fail or, worse,
        // overwrite the new owner's progress.
        partitionsLost.forEach(partition -> {
            inFlight.remove(partition);
            pendingOffsets.remove(partition);
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
