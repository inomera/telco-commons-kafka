package com.inomera.telco.commons.springkafka.consumer.executor;

import com.inomera.telco.commons.springkafka.consumer.executor.virtual.PartitionKeyAwareVirtualExecutorStrategy;
import com.inomera.telco.commons.springkafka.util.ThreadPoolExecutorUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A thread pool execution strategy optimized for CPU-bound tasks using OS threads.
 * <p>
 * This strategy is suitable for computationally intensive tasks such as complex
 * calculations, data transformations, and parallel processing that require multiple OS threads.
 * </p>
 *
 * <b>Usage Guidelines:</b>
 * <ul>
 *     <li>Use {@link SinglePoolExecutorStrategy} for **CPU-bound** scenarios (intensive computations, multi-threaded processing) → OS Threads.</li>
 *     <li>Use {@link PartitionKeyAwareVirtualExecutorStrategy} for **IO-bound** scenarios (DB queries, API calls, messaging) → Virtual Threads.</li>
 * </ul>
 *
 * <b>Key Features:</b>
 * <ul>
 *     <li>Uses a **fixed-size thread pool** for predictable performance.</li>
 *     <li>Supports dynamic reconfiguration of thread pool parameters.</li>
 *     <li>Gracefully shuts down and cleans up resources when no longer needed.</li>
 * </ul>
 *
 * @author Serdar Kuzucu
 * @author Turgay Can
 */
public class SinglePoolExecutorStrategy implements ExecutorStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(SinglePoolExecutorStrategy.class);

    /** The thread pool executor handling task execution. */
    private ThreadPoolExecutor threadPoolExecutor;

    /** The initial number of core threads in the pool. */
    private int coreThreadCount;

    /** The maximum number of threads in the pool. */
    private int maxThreadCount;

    /** The amount of time threads may remain idle before being terminated. */
    private int keepAliveTime;

    /** The time unit for the keep-alive setting. */
    private TimeUnit keepAliveTimeUnit;

    /** Supplier for the task queue used by the executor. */
    private final Supplier<BlockingQueue<Runnable>> queueSupplier;

    /** Factory for creating new worker threads. */
    private final ThreadFactory threadFactory;

    /**
     * Constructs an instance of {@code SinglePoolExecutorStrategy} with the given thread pool configuration.
     *
     * @param coreThreadCount    The number of core threads in the pool.
     * @param maxThreadCount     The maximum number of threads in the pool.
     * @param keepAliveTime      The idle timeout for non-core threads.
     * @param keepAliveTimeUnit  The time unit for the keep-alive setting.
     * @param threadFactory      The factory for creating new threads.
     * @param queueSupplier      Supplier for the blocking queue that holds tasks.
     */
    public SinglePoolExecutorStrategy(int coreThreadCount, int maxThreadCount, int keepAliveTime,
                                      TimeUnit keepAliveTimeUnit, ThreadFactory threadFactory,
                                      Supplier<BlockingQueue<Runnable>> queueSupplier) {
        this.coreThreadCount = coreThreadCount;
        this.maxThreadCount = maxThreadCount;
        this.keepAliveTime = keepAliveTime;
        this.keepAliveTimeUnit = keepAliveTimeUnit;
        this.threadFactory = threadFactory;
        this.queueSupplier = queueSupplier;
    }

    /**
     * Retrieves the thread pool executor for processing Kafka consumer records.
     *
     * @param record The Kafka consumer record.
     * @return The associated {@link ThreadPoolExecutor}.
     */
    @Override
    public ThreadPoolExecutor get(ConsumerRecord<String, ?> record) {
        return threadPoolExecutor;
    }

    /**
     * Initializes the thread pool executor with the configured settings.
     */
    @Override
    public void start() {
        this.threadPoolExecutor = createExecutor();
    }

    /**
     * Gracefully shuts down the thread pool, ensuring all running tasks are completed.
     */
    @Override
    public void stop() {
        ThreadPoolExecutorUtils.closeGracefully(threadPoolExecutor, LOG, getClass().getSimpleName());
    }

    /**
     * Dynamically reconfigures the thread pool with new core and max thread counts.
     *
     * @param coreThreadCount The new core thread count.
     * @param maxThreadCount  The new maximum thread count.
     */
    public void reconfigure(int coreThreadCount, int maxThreadCount) {
        this.coreThreadCount = coreThreadCount;
        this.maxThreadCount = maxThreadCount;
        threadPoolExecutor.setCorePoolSize(coreThreadCount);
        threadPoolExecutor.setMaximumPoolSize(maxThreadCount);
        threadPoolExecutor.prestartAllCoreThreads();
    }

    /**
     * Dynamically reconfigures the thread pool with new settings, including keep-alive time.
     *
     * @param coreThreadCount    The new core thread count.
     * @param maxThreadCount     The new maximum thread count.
     * @param keepAliveTime      The new keep-alive timeout.
     * @param keepAliveTimeUnit  The time unit for the keep-alive setting.
     */
    public void reconfigure(int coreThreadCount, int maxThreadCount, int keepAliveTime, TimeUnit keepAliveTimeUnit) {
        reconfigure(coreThreadCount, maxThreadCount);
        this.keepAliveTime = keepAliveTime;
        this.keepAliveTimeUnit = keepAliveTimeUnit;
        threadPoolExecutor.setKeepAliveTime(keepAliveTime, keepAliveTimeUnit);
        threadPoolExecutor.prestartAllCoreThreads();
    }

    /**
     * Creates and initializes the thread pool executor based on the configured settings.
     *
     * @return A new {@link ThreadPoolExecutor} instance.
     */
    private ThreadPoolExecutor createExecutor() {
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(coreThreadCount, maxThreadCount, keepAliveTime,
                keepAliveTimeUnit, queueSupplier.get(), threadFactory);
        threadPoolExecutor.prestartAllCoreThreads();
        return threadPoolExecutor;
    }
}
