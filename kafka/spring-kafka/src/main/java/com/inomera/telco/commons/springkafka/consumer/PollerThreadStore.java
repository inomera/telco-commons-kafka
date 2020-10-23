package com.inomera.telco.commons.springkafka.consumer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PollerThreadStore implements ConsumerThreadStore {

    private static final Map<Long, PollerThreadState> THREAD_STORE_SET = new ConcurrentHashMap<>();

    @Override
    public void put(Long id, PollerThreadState pollerThreadState) {
        THREAD_STORE_SET.put(id, pollerThreadState);
    }

    @Override
    public Map<Long, PollerThreadState> getThreads() {
        return THREAD_STORE_SET;
    }

}
