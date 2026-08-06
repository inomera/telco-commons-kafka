package com.inomera.telco.commons.springkafka.consumer.retry;

import com.inomera.telco.commons.lang.thread.IncrementalNamingThreadFactory;
import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;
import com.inomera.telco.commons.springkafka.consumer.poller.ConsumerRecordHandler;
import com.inomera.telco.commons.springkafka.util.InterruptUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.inomera.telco.commons.springkafka.SpringKafkaConstants.RETRY_THREAD_NAME_FORMAT;

/**
 * Drains scheduled retries and re-dispatches them through the invoker.
 *
 * <p>Rewritten in 4.1.0. The previous implementation:
 * <ul>
 *   <li>kept its queue in a {@code public static} field, so every consumer in the JVM shared one
 *       queue and a retry could be executed against the wrong consumer group, silently dropping
 *       the record;</li>
 *   <li>re-queued a failure immediately, so {@code retryBackoffTime} never delayed anything and a
 *       permanently failing record was retried as fast as the CPU allowed;</li>
 *   <li>never removed successful records from its attempt-counter map, leaking one entry per
 *       retried record for the lifetime of the process;</li>
 *   <li>ignored the result of {@code offer}, silently discarding records once full;</li>
 *   <li>blocked forever on {@code future.get()} with no timeout.</li>
 * </ul>
 *
 * @author Turgay Can
 */
public class DefaultInMemoryRecordRetryConsumer implements InMemoryRecordRetryConsumer, Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultInMemoryRecordRetryConsumer.class);
    private static final long STOP_TIMEOUT_SECONDS = 15L;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final DelayQueue<RetryContext> retryQueue = new DelayQueue<>();
    private final ConsumerRecordHandler consumerRecordHandler;
    private final RetryPolicy defaultPolicy;
    private final DeadLetterHandler deadLetterHandler;
    private final Semaphore capacity;

    private ExecutorService executorService;
    private volatile RetryOutcomeListener retryOutcomeListener = RetryOutcomeListener.NO_OP;

    public DefaultInMemoryRecordRetryConsumer(ConsumerRecordHandler consumerRecordHandler) {
        this(consumerRecordHandler, RetryPolicy.defaults(), new LoggingDeadLetterHandler());
    }

    public DefaultInMemoryRecordRetryConsumer(ConsumerRecordHandler consumerRecordHandler,
                                              RetryPolicy defaultPolicy,
                                              DeadLetterHandler deadLetterHandler) {
        this.consumerRecordHandler = consumerRecordHandler;
        this.defaultPolicy = defaultPolicy;
        this.deadLetterHandler = deadLetterHandler;
        this.capacity = new Semaphore(defaultPolicy.capacity());
    }

    @Override
    public void setRetryOutcomeListener(RetryOutcomeListener listener) {
        this.retryOutcomeListener = listener == null ? RetryOutcomeListener.NO_OP : listener;
    }

    @Override
    public boolean reportsRetryOutcome() {
        return true;
    }

    @Override
    public boolean offer(InvokerResult result, RetryPolicy policy) {
        if (result == null || result.getRecord() == null) {
            return false;
        }
        final RetryPolicy effective = policy == null ? defaultPolicy : policy;

        // Hard cap: refusing here is what keeps the retry buffer from growing without bound.
        // The caller decides what to do instead (hold the offset back, or dead-letter).
        if (!capacity.tryAcquire()) {
            LOG.warn("In-memory retry capacity ({}) exhausted; refusing retry for {}-{}@{}",
                    effective.capacity(), result.getRecord().topic(),
                    result.getRecord().partition(), result.getRecord().offset());
            return false;
        }

        final RetryContext context = new RetryContext();
        context.setRetry(result);
        context.setCount(1);
        context.setMaxCount(effective.maxAttempts());
        context.setBackoffTime(effective.baseBackoff().toMillis());
        context.scheduleIn(effective.backoffNanos(1));

        if (!retryQueue.offer(context)) {
            capacity.release();
            return false;
        }
        return true;
    }

    @Override
    public void run() {
        try {
            while (running.get()) {
                // Parks until the next attempt is actually due; no spinning, no busy retry.
                final RetryContext context = retryQueue.poll(200, TimeUnit.MILLISECONDS);
                if (context == null) {
                    continue;
                }
                try {
                    consume(context);
                } finally {
                    capacity.release();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.error("In-memory retry loop terminated unexpectedly: {}", e.getMessage(), e);
            InterruptUtils.interruptIfInterruptedException(e);
        }
    }

    @Override
    public void consume(RetryContext retryContext) {
        final InvokerResult result = retryContext.getRetry();
        final ConsumerRecord<String, ?> record = result.getRecord();
        final int attempt = retryContext.getCount();

        LOG.info("Retry attempt {}/{} for {}-{}@{}", attempt, retryContext.getMaxCount(),
                record.topic(), record.partition(), record.offset());

        final boolean failedAgain;
        try {
            final Future<InvokerResult> future = consumerRecordHandler.handle(record);
            final InvokerResult retried = future.get(defaultPolicy.invocationTimeout().toMillis(),
                    TimeUnit.MILLISECONDS);
            // A non-null listener annotation is how the invoker reports "this threw".
            failedAgain = retried != null && retried.getKafkaListener() != null;
        } catch (TimeoutException e) {
            LOG.warn("Retry attempt {} for {}-{}@{} timed out after {}", attempt, record.topic(),
                    record.partition(), record.offset(), defaultPolicy.invocationTimeout());
            reschedule(retryContext, attempt);
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        } catch (Exception e) {
            LOG.error("Retry attempt {} for {}-{}@{} failed: {}", attempt, record.topic(),
                    record.partition(), record.offset(), e.getMessage(), e);
            reschedule(retryContext, attempt);
            return;
        }

        if (failedAgain) {
            reschedule(retryContext, attempt);
            return;
        }
        resolve(record);
    }

    private void reschedule(RetryContext context, int attempt) {
        if (attempt >= context.getMaxCount()) {
            deadLetter(context, attempt);
            return;
        }
        final int next = attempt + 1;
        context.setCount(next);
        context.scheduleIn(defaultPolicy.backoffNanos(next));
        if (capacity.tryAcquire()) {
            if (!retryQueue.offer(context)) {
                capacity.release();
                deadLetter(context, attempt);
            }
        } else {
            deadLetter(context, attempt);
        }
    }

    private void deadLetter(RetryContext context, int attempt) {
        final ConsumerRecord<String, ?> record = context.getRetry().getRecord();
        deadLetterHandler.onExhausted(record, attempt);
        // Terminal either way: the dead-letter handler owns the record now, so the poller may
        // stop withholding the offset. Re-delivering it would only dead-letter it again.
        resolve(record);
    }

    /**
     * Signals that the record is no longer being retried, so its partition can commit again.
     * Never let a listener failure escape: this runs on the retry thread and an exception here
     * would kill the drain loop.
     */
    private void resolve(ConsumerRecord<String, ?> record) {
        try {
            retryOutcomeListener.onRetryResolved(record);
        } catch (Exception e) {
            LOG.error("Retry outcome listener failed for {}-{}@{}: {}",
                    record.topic(), record.partition(), record.offset(), e.getMessage(), e);
            InterruptUtils.interruptIfInterruptedException(e);
        }
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        this.executorService = new ThreadPoolExecutor(0, 1, 0, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(), new IncrementalNamingThreadFactory(RETRY_THREAD_NAME_FORMAT));
        // running is already true, so the loop cannot observe a stale false and exit immediately.
        executorService.submit(this);
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        final ExecutorService executor = this.executorService;
        this.executorService = null;
        if (executor == null) {
            return;
        }
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                LOG.warn("In-memory retry consumer did not stop within {}s.", STOP_TIMEOUT_SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        retryQueue.clear();
    }

    /** Retries currently scheduled. Exposed for metrics and tests. */
    public int pendingRetryCount() {
        return retryQueue.size();
    }
}
