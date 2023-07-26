package com.inomera.telco.commons.springkafka.consumer.retry;

public interface InMemoryBulkRecordRetryConsumer {

    void start();

    void consume(BulkRetryContext retryContext);

    void stop();
}
