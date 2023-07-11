package com.inomera.telco.commons.springkafka.consumer.retry;

import com.inomera.telco.commons.springkafka.consumer.invoker.BulkInvokerResult;

public interface BulkRecordRetryer {

    void checkAndRetry(BulkInvokerResult result);
}
