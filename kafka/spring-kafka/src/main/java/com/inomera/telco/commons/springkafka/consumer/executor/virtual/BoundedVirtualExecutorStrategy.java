package com.inomera.telco.commons.springkafka.consumer.executor.virtual;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Virtual-thread strategy with a hard ceiling on concurrent listener invocations.
 *
 * <h2>Why this exists</h2>
 * A thread-per-task executor never rejects work, so on the virtual path
 * {@code RejectedExecutionException} is never thrown and the rejection handler is effectively
 * dead code. That leaves {@code autoPartitionPause} as the only brake, and it only engages
 * <em>after</em> the whole poll batch has been dispatched. With {@code max.poll.records=1000}
 * that means up to a thousand concurrent invocations per partition, each holding a database or
 * HTTP connection — cheap in threads, expensive everywhere else.
 *
 * <p>This strategy caps in-flight invocations. Once the cap is reached the executor rejects,
 * which routes through the normal rejection path: the partition is paused and the record is
 * re-submitted when capacity frees up.
 *
 * @author Turgay Can
 */
public class BoundedVirtualExecutorStrategy implements DefaultVirtualExecutorStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(BoundedVirtualExecutorStrategy.class);
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 30L;

    private final int maxConcurrency;
    private volatile BoundedExecutor executor;

    public BoundedVirtualExecutorStrategy(int maxConcurrency) {
        if (maxConcurrency < 1) {
            throw new IllegalArgumentException("maxConcurrency must be >= 1, got " + maxConcurrency);
        }
        this.maxConcurrency = maxConcurrency;
    }

    @Override
    public ExecutorService get() {
        final BoundedExecutor current = this.executor;
        if (current == null) {
            throw new IllegalStateException("BoundedVirtualExecutorStrategy has not been started");
        }
        return current;
    }

    @Override
    public void start() {
        this.executor = new BoundedExecutor(maxConcurrency);
        LOG.info("BoundedVirtualExecutorStrategy started with maxConcurrency={}", maxConcurrency);
    }

    @Override
    public void stop() {
        final BoundedExecutor current = this.executor;
        this.executor = null;
        if (current == null) {
            return;
        }
        current.shutdown();
        try {
            if (!current.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                current.shutdownNow();
            }
        } catch (InterruptedException e) {
            current.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOG.info("BoundedVirtualExecutorStrategy stopped");
    }

    /** In-flight invocations right now. Useful as a saturation metric. */
    public int inFlight() {
        final BoundedExecutor current = this.executor;
        return current == null ? 0 : current.inFlight();
    }

    /**
     * Wraps a virtual-thread-per-task executor with a permit semaphore, so that exceeding the
     * cap surfaces as {@link RejectedExecutionException} exactly like a saturated pool.
     */
    private static final class BoundedExecutor extends AbstractExecutorService {
        private final ExecutorService delegate = Executors.newVirtualThreadPerTaskExecutor();
        private final Semaphore permits;
        private final int capacity;

        private BoundedExecutor(int capacity) {
            this.capacity = capacity;
            this.permits = new Semaphore(capacity);
        }

        int inFlight() {
            return capacity - permits.availablePermits();
        }

        @Override
        public void execute(Runnable command) {
            if (!permits.tryAcquire()) {
                throw new RejectedExecutionException(
                        "Bounded virtual executor saturated at " + capacity + " concurrent invocations");
            }
            try {
                delegate.execute(() -> {
                    try {
                        command.run();
                    } finally {
                        permits.release();
                    }
                });
            } catch (RuntimeException | Error e) {
                permits.release();
                throw e;
            }
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }
    }
}
