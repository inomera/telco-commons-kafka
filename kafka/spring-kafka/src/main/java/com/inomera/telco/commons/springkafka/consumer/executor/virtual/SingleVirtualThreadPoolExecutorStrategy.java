package com.inomera.telco.commons.springkafka.consumer.executor.virtual;

import com.inomera.telco.commons.springkafka.consumer.executor.SinglePoolExecutorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * A strategy for executing tasks using a single Virtual Thread-based executor.
 * <p>
 * This class leverages Java 23 Virtual Threads (Project Loom) to efficiently handle IO-bound workloads.
 * It provides a single-threaded virtual executor for tasks such as database queries, API calls, and messaging.
 * </p>
 *
 * <b>Usage Guidelines:</b>
 * <ul>
 *     <li>Use {@link SingleVirtualThreadPoolExecutorStrategy} for **IO-bound** scenarios (DB queries, API calls, messaging) → Virtual Threads.</li>
 *     <li>Use {@link SinglePoolExecutorStrategy} for **CPU-bound** scenarios (intensive computations, multi-threaded processing) → OS Threads.</li>
 * </ul>
 *
 * <b>Key Features:</b>
 * <ul>
 *     <li>Creates a single Virtual Thread-based executor for task execution.</li>
 *     <li>Dynamically starts and stops the executor when required.</li>
 *     <li>Gracefully handles executor shutdown, ensuring all tasks are completed.</li>
 * </ul>
 *
 * @author Turgay Can
 */
public class SingleVirtualThreadPoolExecutorStrategy implements DefaultVirtualExecutorStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(SingleVirtualThreadPoolExecutorStrategy.class);

    /**
     * The Virtual Thread-based ExecutorService.
     */
    private ExecutorService executorService;

    /**
     * Initializes a new instance with a Virtual Thread-based executor.
     * Uses {@link Executors#newVirtualThreadPerTaskExecutor()} to create a per-task virtual thread.
     */
    private final ThreadFactory threadFactory;

    public SingleVirtualThreadPoolExecutorStrategy(ThreadFactory threadFactory) {
        // Validated, not trusted: a factory returning already-started threads makes every task
        // fail with IllegalThreadStateException.
        this.threadFactory = VirtualThreadFactories.unstartedVirtual(threadFactory, "single-virtual-");
        this.executorService = Executors.newThreadPerTaskExecutor(this.threadFactory);
    }

    /**
     * Retrieves the Virtual Thread-based executor.
     *
     * @return The {@link ExecutorService} instance using Virtual Threads.
     */
    @Override
    public ExecutorService get() {
        return executorService;
    }

    /**
     * Starts the Virtual Thread executor if it is not already running.
     * If the executor has been shut down, a new instance is created.
     */
    @Override
    public void start() {
        if (executorService == null || executorService.isShutdown()) {
            // Reuse the validated factory so thread naming survives a stop/start cycle; this used
            // to drop it and fall back to an anonymous executor.
            executorService = Executors.newThreadPerTaskExecutor(threadFactory);
        }
        LOG.info("Starting virtual thread pool");
    }

    /**
     * Stops the Virtual Thread executor by shutting it down gracefully.
     * If tasks do not complete within 5 seconds, the executor is forcibly shut down.
     */
    @Override
    public void stop() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        LOG.info("Stopping virtual thread pool");
    }
}
