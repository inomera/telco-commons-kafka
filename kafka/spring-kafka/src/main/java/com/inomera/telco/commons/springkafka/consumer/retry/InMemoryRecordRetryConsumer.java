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
}
