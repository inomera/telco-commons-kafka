package com.inomera.telco.commons.springkafka.consumer.retry;

import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;

public interface RecordRetryer {

    void checkAndRetry(InvokerResult result);

    /**
     * Applies the retry policy and reports whether the record's offset may be committed.
     *
     * @return {@code false} to hold the offset back so the broker re-delivers the record later.
     *         Implementations must never throw: an exception raised here used to unwind the
     *         poll loop and kill the consumer thread.
     */
    default boolean checkAndDecide(InvokerResult result) {
        checkAndRetry(result);
        return true;
    }
}
