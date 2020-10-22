package com.inomera.telco.commons.springkafka.consumer;

import java.util.Map;

public interface ConsumerThreadStore {

    void put(Long id, PollerThreadState pollerThreadState);

    Map<Long, PollerThreadState> getThreads();
}
