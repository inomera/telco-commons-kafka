package com.inomera.telco.commons.springkafka.consumer.retry;

import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;

public interface RecordRetryer {

    void checkAndRetry(InvokerResult result);
}
