package com.inomera.telco.commons.springkafka.consumer.retry;

import com.inomera.telco.commons.springkafka.consumer.invoker.BulkInvokerResult;

public interface InMemoryBulkRecordRetryConsumer {

    void start();

    void consume(BulkRetryContext retryContext);

    void stop();

    /**
     * Schedules the first retry attempt for a failed batch.
     *
     * @return {@code false} when the retry could not be accepted, typically because the
     *         in-memory capacity is exhausted. Callers must not treat the batch as retried.
     */
    default boolean offer(BulkInvokerResult result, RetryPolicy policy) {
        return false;
    }
}
