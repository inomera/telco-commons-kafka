package com.inomera.telco.commons.springkafka.consumer;

import com.inomera.telco.commons.springkafka.consumer.poller.ConsumerPoller;

import java.util.Collections;
import java.util.Map;

/**
 * @author Serdar Kuzucu
 */
public class NoopConsumerThreadStore implements ConsumerThreadStore {
    @Override
    public void put(ConsumerPoller consumerPoller, Thread thread) {
        // Do nothing.
    }

    @Override
    public Map<Long, PollerThreadState> getThreads() {
        // Return empty map
        return Collections.emptyMap();
    }
}
