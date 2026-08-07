package com.inomera.telco.commons.springkafka.consumer.retry;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Notified when an in-memory retry reaches a terminal state.
 *
 * <p>This is what lets {@code RETRY_IN_MEMORY_TASK} keep the at-least-once guarantee. Accepting
 * a record for retry used to advance the offset immediately, so the broker considered the record
 * consumed while the only surviving copy lived in this process's heap: a pod restart, an OOM or
 * a rolling deploy lost it outright. The poller now pins the partition's committed offset until
 * the retry resolves, and this callback is what unpins it.
 *
 * <p>"Resolved" means terminal, not successful. A dead-lettered record is resolved too, because
 * the {@link DeadLetterHandler} has taken ownership of it and re-delivering it from the broker
 * would only dead-letter it again.
 *
 * @author Turgay Can
 */
@FunctionalInterface
public interface RetryOutcomeListener {

    /** No-op listener, used when nothing is tracking retry outcomes. */
    RetryOutcomeListener NO_OP = record -> {
    };

    /**
     * @param record the record that is no longer being retried. For a bulk retry this is the
     *               record carrying the highest offset in the batch, which is what the poller
     *               pinned the partition at.
     */
    void onRetryResolved(ConsumerRecord<String, ?> record);
}
