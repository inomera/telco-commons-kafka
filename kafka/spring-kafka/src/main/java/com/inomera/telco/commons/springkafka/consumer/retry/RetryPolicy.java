package com.inomera.telco.commons.springkafka.consumer.retry;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Backoff and capacity settings for in-memory retries.
 *
 * <p>The delay grows exponentially and is then jittered into {@code [50%, 100%]} of the
 * computed value, so that many consumers failing against the same downstream do not line up
 * and retry in lockstep.
 *
 * @param maxAttempts       how many times a record may be retried before it is dead-lettered
 * @param baseBackoff       delay before the first retry
 * @param maxBackoff        ceiling for the exponential growth
 * @param multiplier        growth factor per attempt
 * @param invocationTimeout how long a single retry invocation may run before it is abandoned
 * @param capacity          hard ceiling on retries held in memory at once
 * @author Turgay Can
 */
public record RetryPolicy(int maxAttempts,
                          Duration baseBackoff,
                          Duration maxBackoff,
                          double multiplier,
                          Duration invocationTimeout,
                          int capacity) {

    public RetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1, got " + maxAttempts);
        }
        if (multiplier < 1.0d) {
            throw new IllegalArgumentException("multiplier must be >= 1.0, got " + multiplier);
        }
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1, got " + capacity);
        }
        if (baseBackoff.isNegative() || maxBackoff.isNegative()) {
            throw new IllegalArgumentException("backoff durations must not be negative");
        }
    }

    public static RetryPolicy defaults() {
        return new RetryPolicy(3, Duration.ofMillis(500), Duration.ofSeconds(30), 2.0d,
                Duration.ofSeconds(30), 10_000);
    }

    /**
     * Builds a policy from the values carried on a {@code @KafkaListener}, keeping the
     * annotation's {@code retryCount} and {@code retryBackoffTime} meaningful.
     */
    public static RetryPolicy fromListener(int retryCount, long retryBackoffTimeMillis) {
        final Duration base = Duration.ofMillis(Math.max(1L, retryBackoffTimeMillis));
        final RetryPolicy defaults = defaults();
        return new RetryPolicy(Math.max(1, retryCount), base,
                base.multipliedBy(8).compareTo(defaults.maxBackoff()) > 0 ? base.multipliedBy(8) : defaults.maxBackoff(),
                defaults.multiplier(), defaults.invocationTimeout(), defaults.capacity());
    }

    /**
     * Delay before the given attempt, exponential with jitter.
     *
     * @param attempt 1-based attempt number
     */
    public long backoffNanos(int attempt) {
        final double raw = baseBackoff.toNanos() * Math.pow(multiplier, Math.max(0, attempt - 1));
        final long capped = (long) Math.min(raw, (double) maxBackoff.toNanos());
        final long half = capped / 2;
        if (half <= 0) {
            return capped;
        }
        return half + ThreadLocalRandom.current().nextLong(half + 1);
    }
}
