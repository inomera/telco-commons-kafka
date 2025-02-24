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
 * <h3>Usage Guidelines:</h3>
 * <ul>
 *     <li>Use {@link SingleVirtualThreadPoolExecutorStrategy} for **IO-bound** scenarios (DB queries, API calls, messaging) → Virtual Threads.</li>
 *     <li>Use {@link SinglePoolExecutorStrategy} for **CPU-bound** scenarios (intensive computations, multi-threaded processing) → OS Threads.</li>
 * </ul>
 *
 * <h3>Key Features:</h3>
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
    public SingleVirtualThreadPoolExecutorStrategy(ThreadFactory threadFactory) {
        this.executorService = Executors.newThreadPerTaskExecutor(threadFactory);
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
            executorService = Executors.newVirtualThreadPerTaskExecutor();
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
