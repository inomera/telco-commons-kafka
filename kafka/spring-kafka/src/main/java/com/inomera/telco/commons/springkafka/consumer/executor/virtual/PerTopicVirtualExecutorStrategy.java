package com.inomera.telco.commons.springkafka.consumer.executor.virtual;

import com.inomera.telco.commons.lang.thread.IncrementalNamingVirtualThreadFactory;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A Kafka Consumer execution strategy that assigns a dedicated Virtual Thread pool per topic.
 * <p>
 * This strategy ensures that each Kafka topic is processed using its own Virtual Thread-based executor.
 * It is optimized for high-concurrency IO-bound workloads such as API calls, database queries, and messaging.
 * </p>
 *
 * <b>Key Features:</b>
 * <ul>
 *     <li>Uses Java 23 Virtual Threads (Project Loom) for lightweight, scalable concurrency.</li>
 *     <li>Ensures topic-based isolation, where each topic gets its own executor.</li>
 *     <li>Dynamically creates Virtual Threads on demand, reducing resource contention.</li>
 * </ul>
 *
 * <b>Usage Guidelines:</b>
 * <ul>
 *     <li>For **IO-bound tasks** (e.g., DB queries, API calls), this strategy leverages Virtual Threads.</li>
 *     <li>For **CPU-bound tasks**, consider using an OS thread-based executor.</li>
 * </ul>
 *
 * @author Turgay Can
 */
@RequiredArgsConstructor
public class PerTopicVirtualExecutorStrategy implements ParameterBasedVirtualExecutorStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(PerTopicVirtualExecutorStrategy.class);

    /**
     * A concurrent map to store a dedicated ExecutorService for each Kafka topic.
     */
    private final Map<String, ExecutorService> executorMap = new ConcurrentHashMap<>();

    /**
     * Lock object for synchronizing executor shutdown operations.
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final ThreadFactory threadFactory;

    /**
     * Retrieves the {@link ExecutorService} responsible for processing the given Kafka record.
     * Ensures that records belonging to the same topic are processed by the same Virtual Thread pool.
     *
     * @param record The Kafka {@link ConsumerRecord} being processed.
     * @return An {@link ExecutorService} bound to the topic of the record.
     */
    @Override
    public ExecutorService get(ConsumerRecord<String, ?> record) {
        return executorMap.computeIfAbsent(record.topic(), this::createExecutor);
    }

    /**
     * Initializes the Virtual Thread execution strategy.
     * No pre-allocation is required, as Virtual Threads are created dynamically on demand.
     */
    @Override
    public void start() {
        LOG.info("Starting VirtualExecutorPerTopicStrategy");
    }

    /**
     * Gracefully shuts down all topic-specific Virtual Thread executors.
     * Ensures all ongoing tasks are completed before termination.
     */
    @Override
    public void stop() {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            executorMap.values().forEach(ExecutorService::shutdown);
            executorMap.clear();
        } finally {
            writeLock.unlock();
        }
        LOG.info("VirtualExecutorPerTopicStrategy stopped");
    }

    /**
     * Creates a new Virtual Thread-based executor for a given Kafka topic.
     *
     * @param topic The Kafka topic name.
     * @return A new {@link ExecutorService} that utilizes Virtual Threads.
     */
    private ExecutorService createExecutor(String topic) {
        LOG.info("Creating new Virtual Thread Pool for topic: {}", topic);
        if (this.threadFactory != null) {
            return Executors.newThreadPerTaskExecutor(this.threadFactory);
        }
        IncrementalNamingVirtualThreadFactory virtualThreadFactory = new IncrementalNamingVirtualThreadFactory(topic);
        return Executors.newThreadPerTaskExecutor(virtualThreadFactory);
    }
}
