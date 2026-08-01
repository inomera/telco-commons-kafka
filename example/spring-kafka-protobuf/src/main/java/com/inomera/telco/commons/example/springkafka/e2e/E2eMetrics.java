package com.inomera.telco.commons.example.springkafka.e2e;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Counters shared between the E2E scenario endpoints and the scenario listeners, so
 * {@code GET /e2e/status} can report what actually happened.
 */
public class E2eMetrics {

    private final AtomicLong produced = new AtomicLong();
    private final AtomicLong poisonProduced = new AtomicLong();
    private final AtomicLong retryTopicProduced = new AtomicLong();
    private final AtomicLong retryAttempts = new AtomicLong();
    private final AtomicLong consumerRestarts = new AtomicLong();

    /** Nanotime of each retry attempt, so backoff spacing is observable. */
    private final ConcurrentLinkedQueue<Long> retryAttemptNanos = new ConcurrentLinkedQueue<>();

    public void addProduced(long count) {
        produced.addAndGet(count);
    }

    public void addPoisonProduced(long count) {
        poisonProduced.addAndGet(count);
    }

    public void addRetryTopicProduced(long count) {
        retryTopicProduced.addAndGet(count);
    }

    public long recordRetryAttempt() {
        retryAttemptNanos.add(System.nanoTime());
        return retryAttempts.incrementAndGet();
    }

    public void recordConsumerRestart() {
        consumerRestarts.incrementAndGet();
    }

    /** Gaps between consecutive retry attempts, in milliseconds. */
    public List<Long> retryGapsMillis() {
        final Long[] stamps = retryAttemptNanos.toArray(new Long[0]);
        final List<Long> gaps = new ArrayList<>(Math.max(0, stamps.length - 1));
        for (int i = 1; i < stamps.length; i++) {
            gaps.add((stamps[i] - stamps[i - 1]) / 1_000_000L);
        }
        return gaps;
    }

    public Map<String, Object> snapshot() {
        final Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("produced", produced.get());
        snapshot.put("poisonProduced", poisonProduced.get());
        snapshot.put("retryTopicProduced", retryTopicProduced.get());
        snapshot.put("retryAttempts", retryAttempts.get());
        snapshot.put("retryGapsMillis", retryGapsMillis());
        snapshot.put("consumerRestarts", consumerRestarts.get());
        return snapshot;
    }

    public void reset() {
        produced.set(0);
        poisonProduced.set(0);
        retryTopicProduced.set(0);
        retryAttempts.set(0);
        consumerRestarts.set(0);
        retryAttemptNanos.clear();
    }
}
