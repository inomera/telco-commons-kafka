package com.inomera.telco.commons.springkafka.consumer;

import com.inomera.telco.commons.springkafka.consumer.poller.ConsumerPoller;

import java.util.Map;

public interface ConsumerThreadStore {
    void put(ConsumerPoller consumerPoller, Thread thread);

    Map<Long, PollerThreadState> getThreads();
}
