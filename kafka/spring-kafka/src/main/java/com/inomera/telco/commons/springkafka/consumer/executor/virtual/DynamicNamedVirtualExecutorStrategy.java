package com.inomera.telco.commons.springkafka.consumer.executor.virtual;

import com.inomera.telco.commons.lang.thread.IncrementalNamingUnstartedVirtualThreadFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;

/**
 * Dynamic Executor Strategy that assigns Kafka Consumer records to dynamically created Virtual Thread Pools per topic.
 * <p>
 * This strategy ensures that each topic or business key has its own Virtual Thread Pool.
 * </p>
 *
 * <b>Usage Guidelines:</b>
 * <ul>
 *     <li>Fully based on **Virtual Threads** (for IO-bound workloads like DB queries, API calls, messaging).</li>
 *     <li>Automatically assigns a new Virtual Thread per Kafka record.</li>
 *     <li>Ensures each executor is dynamically created on demand.</li>
 * </ul>
 *
 * @author Turgay Can
 */
public class DynamicNamedVirtualExecutorStrategy implements ParameterBasedVirtualExecutorStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(DynamicNamedVirtualExecutorStrategy.class);
    private static final String DEFAULT_EXECUTOR_NAME = "DefaultExecutor";

    /**
     * Stores dynamically created named executors
     */
    private final Map<String, ExecutorService> executorMap = new ConcurrentHashMap<>();

    /**
     * Maps records to executor names
     */
    private final Function<ConsumerRecord<String, ?>, String> recordExecutorNameMapper;

    /**
     * Synchronization lock
     */
    private final StampedLock lock = new StampedLock();

    /**
     * Default executor
     */
    private ExecutorService defaultExecutor;

    /**
     * Constructs a dynamic executor strategy.
     *
     * @param recordExecutorNameMapper Function to determine executor names.
     */
    public DynamicNamedVirtualExecutorStrategy(Function<ConsumerRecord<String, ?>, String> recordExecutorNameMapper) {
        this.recordExecutorNameMapper = recordExecutorNameMapper;
    }

    /**
     * Retrieves an executor for a given Kafka record.
     * If the executor does not exist, it is created dynamically.
     *
     * @param record The Kafka record.
     * @return The assigned {@link ExecutorService}.
     */
    @Override
    public ExecutorService get(ConsumerRecord<String, ?> record) {
        final String executorName = recordExecutorNameMapper.apply(record);
        return getOrCreateByExecutorName(executorName);
    }

    /**
     * Retrieves or creates an executor by name.
     *
     * @param executorName The executor name.
     * @return The corresponding {@link ExecutorService}.
     */
    public ExecutorService getOrCreateByExecutorName(String executorName) {
        return executorMap.computeIfAbsent(executorName, this::createVirtualThreadExecutor);
    }

    /**
     * Removes an executor by name.
     *
     * @param executorName The executor name.
     */
    public void removeExecutor(String executorName) {
        removeAndStop(executorName);
        LOG.info("Removed executor [{}]", executorName);
    }

    /**
     * Stops and removes an executor.
     *
     * @param executorName The executor name.
     */
    private void removeAndStop(String executorName) {
        long stamp = lock.writeLock();
        try {
            try (ExecutorService removedExecutor = executorMap.remove(executorName)) {
                if (removedExecutor != null) {
                    removedExecutor.shutdown();
                }
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Starts the default executor.
     */
    @Override
    public void start() {
        long stamp = lock.writeLock();
        try {
            defaultExecutor = createVirtualThreadExecutor(DEFAULT_EXECUTOR_NAME);
        } finally {
            lock.unlockWrite(stamp);
        }
        LOG.info("DynamicNamedVirtualExecutorStrategy started");
    }

    /**
     * Stops all executors.
     */
    @Override
    public void stop() {
        long stamp = lock.writeLock();
        try {
            executorMap.values().forEach(ExecutorService::shutdown);
            if (defaultExecutor != null) {
                defaultExecutor.shutdown();
            }
            executorMap.clear();
        } finally {
            lock.unlockWrite(stamp);
        }
        LOG.info("DynamicNamedVirtualExecutorStrategy stopped");
    }

    /**
     * Creates a new Virtual Thread-based Executor.
     *
     * @param executorName The executor name.
     * @return A new {@link ExecutorService}.
     */
    private ExecutorService createVirtualThreadExecutor(String executorName) {
        LOG.info("Creating Virtual Thread Executor for: {}", executorName);
        return Executors.newThreadPerTaskExecutor(new IncrementalNamingUnstartedVirtualThreadFactory(executorName));
    }
}
