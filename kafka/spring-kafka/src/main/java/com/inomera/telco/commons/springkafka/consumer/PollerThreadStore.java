package com.inomera.telco.commons.springkafka.consumer;

import com.inomera.telco.commons.springkafka.consumer.poller.ConsumerPoller;
import com.inomera.telco.commons.springkafka.util.HostUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Serdar Kuzucu
 * @author Turgay Can
 */
public class PollerThreadStore implements ConsumerThreadStore {
    private static final Map<Long, PollerThreadState> THREAD_STORE_SET = new ConcurrentHashMap<>();

    @Override
    public void put(ConsumerPoller consumerPoller, Thread thread) {
        final String hostname = HostUtils.getHostname();
        final String hostedThreadName = hostname.concat("-").concat(thread.getName());
        final long threadId = thread.threadId();

        final PollerThreadState pollerThreadState = new PollerThreadState();
        pollerThreadState.setThreadId(threadId);
        pollerThreadState.setHostname(hostname);
        pollerThreadState.setThreadName(hostedThreadName);
        pollerThreadState.setOldJvmState(thread.getState().name());
        pollerThreadState.setCurrentJvmState(thread.getState().name());
        pollerThreadState.setConsumerPoller(consumerPoller);

        THREAD_STORE_SET.put(threadId, pollerThreadState);
    }

    @Override
    public Map<Long, PollerThreadState> getThreads() {
        return THREAD_STORE_SET;
    }

}
