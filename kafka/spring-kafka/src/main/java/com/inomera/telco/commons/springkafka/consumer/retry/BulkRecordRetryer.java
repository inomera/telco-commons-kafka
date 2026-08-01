package com.inomera.telco.commons.springkafka.consumer.retry;

import com.inomera.telco.commons.springkafka.consumer.invoker.BulkInvokerResult;

public interface BulkRecordRetryer {

    void checkAndRetry(BulkInvokerResult result);

    /**
     * Applies the retry policy and reports whether the batch's offsets may be committed.
     *
     * @return {@code false} to hold the offsets back so the broker re-delivers the batch later.
     *         Implementations must never throw.
     */
    default boolean checkAndDecide(BulkInvokerResult result) {
        checkAndRetry(result);
        return true;
    }
}
