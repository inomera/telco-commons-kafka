package com.inomera.telco.commons.springkafka.consumer.invoker.virtual;

import com.inomera.telco.commons.lang.thread.IncrementalNamingThreadFactory;
import com.inomera.telco.commons.springkafka.consumer.executor.virtual.VirtualExecutorStrategy;
import com.inomera.telco.commons.springkafka.consumer.invoker.BulkInvokerResult;
import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;
import com.inomera.telco.commons.springkafka.consumer.invoker.RejectionHandler;
import com.inomera.telco.commons.springkafka.consumer.poller.ConsumerPoller;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Re-submits work that a virtual executor refused.
 *
 * <p>Rewritten in 4.1.0. The previous version offered the rejected task to a
 * {@code static} queue that nothing ever drained. Because the queue was unbounded, the
 * {@code offer} always succeeded, so the re-submit branch was unreachable: the task never ran,
 * its {@code Future} never completed, the poller kept it in flight forever and the partition —
 * paused just above — was never resumed. The queue also grew without bound.
 *
 * <p>The task is now guaranteed to complete: it is retried a bounded number of times and then
 * run inline rather than being dropped.
 *
 * @author Turgay Can
 */
@RequiredArgsConstructor
public class VirtualPauseAndRetryRejectionHandler implements RejectionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(VirtualPauseAndRetryRejectionHandler.class);

    private static final long RESUBMIT_BACKOFF_MILLIS = 20L;
    private static final long STOP_TIMEOUT_SECONDS = 15L;
    /** How long to keep waiting for capacity before completing the task inline regardless. */
    private static final Duration RESUBMIT_DEADLINE = Duration.ofMinutes(5);

    private final ConsumerPoller consumerPoller;
    private final VirtualExecutorStrategy executorStrategy;

    private volatile ExecutorService retryExecutor;

    @Override
    public void start() {
        retryExecutor = Executors.newThreadPerTaskExecutor(
                new IncrementalNamingThreadFactory("virtual-invoker-rejection-retry-"));
    }

    @Override
    public void stop() {
        final ExecutorService executor = this.retryExecutor;
        this.retryExecutor = null;
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void handleReject(final ConsumerRecord<String, ?> record, final FutureTask<InvokerResult> futureTask) {
        final TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
        LOG.info("handleReject::rejected execution of {}", topicPartition);

        consumerPoller.pause(topicPartition);
        resubmit(futureTask, record, topicPartition);
    }

    @Override
    public void handleReject(Set<ConsumerRecord<String, ?>> records, FutureTask<BulkInvokerResult> futureTask) {
        final ConsumerRecord<String, ?> firstRecord = records.iterator().next();
        final TopicPartition topicPartition = new TopicPartition(firstRecord.topic(), firstRecord.partition());
        LOG.info("bulk handleReject::rejected execution of {} records for {}", records.size(), topicPartition);

        consumerPoller.pause(topicPartition);
        resubmit(futureTask, firstRecord, topicPartition);
    }

    private void resubmit(FutureTask<?> futureTask, ConsumerRecord<String, ?> record, TopicPartition partition) {
        final ExecutorService executor = this.retryExecutor;
        if (executor == null) {
            // Shutting down: run inline so the poller is never left waiting on a dead future.
            futureTask.run();
            return;
        }
        try {
            executor.execute(new ResubmitTask(futureTask, record, partition));
        } catch (RejectedExecutionException e) {
            futureTask.run();
        }
    }

    private final class ResubmitTask implements Runnable {
        private final FutureTask<?> futureTask;
        private final ConsumerRecord<String, ?> record;
        private final TopicPartition partition;

        private ResubmitTask(FutureTask<?> futureTask, ConsumerRecord<String, ?> record, TopicPartition partition) {
            this.futureTask = futureTask;
            this.record = record;
            this.partition = partition;
        }

        @Override
        public void run() {
            final long deadlineNanos = System.nanoTime() + RESUBMIT_DEADLINE.toNanos();
            int attempt = 0;

            while (!futureTask.isDone()) {
                final ExecutorService executor;
                try {
                    executor = executorStrategy.getExecutor(record);
                } catch (RuntimeException e) {
                    // Strategy already stopped: run inline so the poller is not left waiting.
                    LOG.debug("Executor unavailable for {}, running inline: {}", partition, e.getMessage());
                    futureTask.run();
                    return;
                }

                if (executor.isShutdown()) {
                    LOG.debug("Executor for {} is shut down, running inline.", partition);
                    futureTask.run();
                    return;
                }

                try {
                    executor.execute(futureTask);
                    return;
                } catch (RejectedExecutionException e) {
                    // Saturated, not dead. Keep waiting: running the task here instead would
                    // execute it on this thread and defeat whatever concurrency cap the
                    // strategy enforces, which is the whole point of a bounded dispatcher.
                    attempt++;
                    LOG.debug("Re-submit attempt {} for {} rejected: {}", attempt, partition, e.getMessage());
                }

                if (System.nanoTime() - deadlineNanos >= 0) {
                    // Something is wedged. Completing the task is more important than the cap,
                    // because the poller never resumes the partition until this future completes.
                    LOG.warn("Running {} inline after {} rejected re-submits over {}.",
                            partition, attempt, RESUBMIT_DEADLINE);
                    futureTask.run();
                    return;
                }

                try {
                    Thread.sleep(RESUBMIT_BACKOFF_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (!futureTask.isDone()) {
                        futureTask.run();
                    }
                    return;
                }
            }
        }
    }
}
