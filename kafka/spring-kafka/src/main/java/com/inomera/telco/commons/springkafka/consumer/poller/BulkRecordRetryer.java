package com.inomera.telco.commons.springkafka.consumer.poller;

import com.inomera.telco.commons.springkafka.consumer.invoker.BulkInvokerResult;

public interface BulkRecordRetryer {

    void checkAndRetry(BulkInvokerResult result);
}
