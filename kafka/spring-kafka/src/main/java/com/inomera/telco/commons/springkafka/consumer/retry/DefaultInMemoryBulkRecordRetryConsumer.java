package com.inomera.telco.commons.springkafka.consumer.retry;

import com.inomera.telco.commons.lang.thread.IncrementalNamingThreadFactory;
import com.inomera.telco.commons.springkafka.consumer.invoker.BulkInvokerResult;
import com.inomera.telco.commons.springkafka.consumer.poller.BulkConsumerRecordHandler;
import com.inomera.telco.commons.springkafka.util.InterruptUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
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
 * Bulk counterpart of {@link DefaultInMemoryRecordRetryConsumer}.
 *
 * <p>The previous implementation had the same defects plus an <em>unbounded</em> static queue,
 * and each entry held a whole partition batch, so a failing downstream could retain every
 * record polled since the failure started.
 *
 * @author Turgay Can
 */
public class DefaultInMemoryBulkRecordRetryConsumer implements InMemoryBulkRecordRetryConsumer, Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultInMemoryBulkRecordRetryConsumer.class);
    private static final long STOP_TIMEOUT_SECONDS = 15L;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final DelayQueue<BulkRetryContext> retryQueue = new DelayQueue<>();
    private final BulkConsumerRecordHandler bulkConsumerRecordHandler;
    private final RetryPolicy defaultPolicy;
    private final DeadLetterHandler deadLetterHandler;
    private final Semaphore capacity;

    private ExecutorService executorService;
    private volatile RetryOutcomeListener retryOutcomeListener = RetryOutcomeListener.NO_OP;

    public DefaultInMemoryBulkRecordRetryConsumer(BulkConsumerRecordHandler bulkConsumerRecordHandler) {
        this(bulkConsumerRecordHandler, RetryPolicy.defaults(), new LoggingDeadLetterHandler());
    }

    public DefaultInMemoryBulkRecordRetryConsumer(BulkConsumerRecordHandler bulkConsumerRecordHandler,
                                                  RetryPolicy defaultPolicy,
                                                  DeadLetterHandler deadLetterHandler) {
        this.bulkConsumerRecordHandler = bulkConsumerRecordHandler;
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
    public boolean offer(BulkInvokerResult result, RetryPolicy policy) {
        if (result == null || result.getRecords() == null || result.getRecords().isEmpty()) {
            return false;
        }
        final RetryPolicy effective = policy == null ? defaultPolicy : policy;

        if (!capacity.tryAcquire()) {
            LOG.warn("In-memory bulk retry capacity ({}) exhausted; refusing retry for a batch of {} records",
                    effective.capacity(), result.getRecords().size());
            return false;
        }

        final BulkRetryContext context = new BulkRetryContext();
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
                final BulkRetryContext context = retryQueue.poll(200, TimeUnit.MILLISECONDS);
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
            LOG.error("In-memory bulk retry loop terminated unexpectedly: {}", e.getMessage(), e);
            InterruptUtils.interruptIfInterruptedException(e);
        }
    }

    @Override
    public void consume(BulkRetryContext retryContext) {
        final BulkInvokerResult result = retryContext.getRetry();
        final Set<ConsumerRecord<String, ?>> records = result.getRecords();
        final int attempt = retryContext.getCount();

        LOG.info("Bulk retry attempt {}/{} for {} records", attempt, retryContext.getMaxCount(), records.size());

        final boolean failedAgain;
        try {
            final Future<BulkInvokerResult> future = bulkConsumerRecordHandler.handle(records);
            final BulkInvokerResult retried = future.get(defaultPolicy.invocationTimeout().toMillis(),
                    TimeUnit.MILLISECONDS);
            failedAgain = retried != null && retried.getKafkaListener() != null;
        } catch (TimeoutException e) {
            LOG.warn("Bulk retry attempt {} timed out after {}", attempt, defaultPolicy.invocationTimeout());
            reschedule(retryContext, attempt);
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        } catch (Exception e) {
            LOG.error("Bulk retry attempt {} failed: {}", attempt, e.getMessage(), e);
            reschedule(retryContext, attempt);
            return;
        }

        if (failedAgain) {
            reschedule(retryContext, attempt);
            return;
        }
        resolve(records);
    }

    private void reschedule(BulkRetryContext context, int attempt) {
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

    private void deadLetter(BulkRetryContext context, int attempt) {
        final Set<ConsumerRecord<String, ?>> records = context.getRetry().getRecords();
        deadLetterHandler.onExhausted(records, attempt);
        resolve(records);
    }

    /**
     * Unpins the batch's partition. The poller pinned it at the batch's <em>highest</em> offset,
     * which is also what it would have committed, so that is the offset to release.
     */
    private void resolve(Set<ConsumerRecord<String, ?>> records) {
        ConsumerRecord<String, ?> highest = null;
        for (ConsumerRecord<String, ?> record : records) {
            if (highest == null || record.offset() > highest.offset()) {
                highest = record;
            }
        }
        if (highest == null) {
            return;
        }
        try {
            retryOutcomeListener.onRetryResolved(highest);
        } catch (Exception e) {
            LOG.error("Retry outcome listener failed for a batch ending at {}-{}@{}: {}",
                    highest.topic(), highest.partition(), highest.offset(), e.getMessage(), e);
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
                LOG.warn("In-memory bulk retry consumer did not stop within {}s.", STOP_TIMEOUT_SECONDS);
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
