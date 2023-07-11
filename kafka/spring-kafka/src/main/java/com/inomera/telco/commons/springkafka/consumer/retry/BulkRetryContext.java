package com.inomera.telco.commons.springkafka.consumer.retry;

import com.inomera.telco.commons.springkafka.consumer.invoker.BulkInvokerResult;
import lombok.Data;

import java.util.Date;

@Data
public class BulkRetryContext {
    private int count;
    private long backoffTime;
    private long offerTime = new Date().getTime();
    private long maxCount;
    private BulkInvokerResult retry;

    public boolean isPassed(long now) {
        return  now >= getOfferTime() + getRetry().getKafkaListener().retryBackoffTime();
    }
}
