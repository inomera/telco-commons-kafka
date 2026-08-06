package com.inomera.telco.commons.springkafka.consumer.retry;

import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;

public interface InMemoryRecordRetryConsumer {

    void start();

    void consume(RetryContext retryContext);

    void stop();

    /**
     * Schedules the first retry attempt for a failed record.
     *
     * @return {@code false} when the retry could not be accepted, typically because the
     *         in-memory capacity is exhausted. Callers must not treat the record as retried.
     */
    default boolean offer(InvokerResult result, RetryPolicy policy) {
        return false;
    }

    /**
     * Registers the callback fired when a retried record reaches a terminal state.
     * Implementations that honour it must also override {@link #reportsRetryOutcome()}.
     */
    default void setRetryOutcomeListener(RetryOutcomeListener listener) {
        // no-op
    }

    /**
     * Whether this implementation actually fires its {@link RetryOutcomeListener}.
     *
     * <p>Defaults to {@code false} so that a custom implementation written against 4.1.x keeps
     * working: the retryer falls back to committing the offset as soon as the retry is accepted.
     * If it returned {@code true} here without ever firing the callback, the partition would
     * stay pinned and stop committing forever.
     */
    default boolean reportsRetryOutcome() {
        return false;
    }
}
