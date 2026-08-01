package com.inomera.telco.commons.springkafka.consumer.retry;

import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * A single scheduled retry attempt.
 *
 * <p>Implements {@link Delayed} so the retry consumer can park on a {@code DelayQueue} until
 * the attempt is actually due. The previous version re-queued failures immediately and only
 * consulted {@code retryBackoffTime} in its exit condition, which meant a permanently failing
 * record was retried as fast as the CPU allowed for the whole backoff window.
 *
 * @author Turgay Can
 */
@Getter
@Setter
public class RetryContext implements Delayed {

    /** 1-based attempt number; incremented each time the record is rescheduled. */
    private int count;
    private long backoffTime;
    private long maxCount;
    private InvokerResult retry;

    /** {@link System#nanoTime()} reading at which this attempt becomes due. */
    private long dueNanos = System.nanoTime();

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(dueNanos - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        if (other instanceof RetryContext context) {
            return Long.compare(this.dueNanos, context.dueNanos);
        }
        return Long.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
    }

    /** Schedules this attempt {@code delayNanos} into the future. */
    public void scheduleIn(long delayNanos) {
        this.dueNanos = System.nanoTime() + delayNanos;
    }
}
