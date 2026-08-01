package com.inomera.telco.commons.springkafka.consumer.retry;

import com.inomera.telco.commons.springkafka.consumer.invoker.BulkInvokerResult;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Bulk counterpart of {@link RetryContext}.
 *
 * @author Turgay Can
 */
@Getter
@Setter
public class BulkRetryContext implements Delayed {

    /** 1-based attempt number; incremented each time the batch is rescheduled. */
    private int count;
    private long backoffTime;
    private long maxCount;
    private BulkInvokerResult retry;

    /** {@link System#nanoTime()} reading at which this attempt becomes due. */
    private long dueNanos = System.nanoTime();

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(dueNanos - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        if (other instanceof BulkRetryContext context) {
            return Long.compare(this.dueNanos, context.dueNanos);
        }
        return Long.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
    }

    /** Schedules this attempt {@code delayNanos} into the future. */
    public void scheduleIn(long delayNanos) {
        this.dueNanos = System.nanoTime() + delayNanos;
    }
}
