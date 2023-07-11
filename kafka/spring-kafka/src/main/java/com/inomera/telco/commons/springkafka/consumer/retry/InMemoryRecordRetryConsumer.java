package com.inomera.telco.commons.springkafka.consumer.retry;

public interface InMemoryRecordRetryConsumer {

    void start();

    void consume(RetryContext retryContext);

    void stop();

}
