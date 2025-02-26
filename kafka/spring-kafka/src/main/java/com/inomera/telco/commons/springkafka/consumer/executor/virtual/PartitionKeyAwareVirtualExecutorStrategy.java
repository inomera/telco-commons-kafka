package com.inomera.telco.commons.springkafka.consumer.executor.virtual;

import com.inomera.telco.commons.springkafka.PartitionKeyAware;
import com.inomera.telco.commons.springkafka.consumer.executor.PartitionKeyAwareExecutorStrategy;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

/**
 * An execution strategy that ensures partition affinity using Virtual Threads.
 * <p>
 * This strategy guarantees that records with the same partition key are processed by the same Virtual Thread.
 * It is optimized for IO-bound workloads such as database queries, API calls, and messaging.
 * </p>
 *
 * <h3>Usage</h3>
 * <ul>
 *     <li>Use {@link PartitionKeyAwareVirtualExecutorStrategy} for IO-bound tasks → Virtual Threads</li>
 *     <li>Use {@link PartitionKeyAwareExecutorStrategy} for CPU-bound tasks → OS Threads</li>
 * </ul>
 *
 * <h3>Behavior</h3>
 * <ul>
 *     <li>Each unique partition key is mapped to a dedicated Virtual Thread Executor.</li>
 *     <li>Ensures ordered processing of records with the same partition key.</li>
 *     <li>Leverages Java 23's Virtual Threads for lightweight concurrency.</li>
 * </ul>
 *
 * @author Turgay Can
 */
@RequiredArgsConstructor
public class PartitionKeyAwareVirtualExecutorStrategy implements ParameterBasedVirtualExecutorStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(PartitionKeyAwareVirtualExecutorStrategy.class);

    /**
     * Number of partition pools, typically correlates with the number of Kafka partitions.
     */
    private final int partitionPoolSize;

    /**
     * A factory for creating new threads.
     * <p>
     * This variable is used to supply threads for partition-specific executors. By encapsulating
     * thread creation in a factory, it allows customization of thread properties such as naming,
     * priority, or daemon status. This contributes to better thread pool management and debugging.
     * <p>
     * Typically, each partition in the {@link PartitionKeyAwareVirtualExecutorStrategy} is assigned
     * threads created by this factory, ensuring a tailored concurrency execution strategy for Kafka
     * records.
     */
    private final ThreadFactory threadFactory;

    /**
     * A map that holds a dedicated Virtual Thread ExecutorService for each partition key.
     */
    private final Map<Integer, ExecutorService> partitionExecutors = new ConcurrentHashMap<>();

    /**
     * Retrieves the {@link ExecutorService} responsible for processing the given record.
     * Ensures that records with the same partition key are assigned to the same Virtual Thread.
     *
     * @param record The Kafka {@link ConsumerRecord} being processed.
     * @return An {@link ExecutorService} bound to the partition key of the record.
     */
    @Override
    public ExecutorService get(ConsumerRecord<String, ?> record) {
        int partitionKey = getPartitionKey(record);
        final int partitionVirtualIndex = Math.abs(partitionKey) % partitionPoolSize;
        return partitionExecutors.computeIfAbsent(partitionVirtualIndex, key ->
                Executors.newSingleThreadExecutor(this.threadFactory));
    }

    /**
     * Initializes the executor pool for partition-based Virtual Thread execution.
     * Prepares dedicated Virtual Thread Executors for each partition index.
     */
    @Override
    public void start() {
        for (int partitionIndex = 0; partitionIndex < partitionPoolSize; partitionIndex++) {
            partitionExecutors.put(partitionIndex, Executors.newSingleThreadExecutor(Thread.ofVirtual().factory()));
            LOG.info("PartitionKeyAwareVirtualExecutorStrategy started for partition {}", partitionIndex);
        }
        LOG.info("PartitionKeyAwareVirtualExecutorStrategy started");
    }

    /**
     * Gracefully shuts down all partition-specific executors.
     * Ensures all ongoing tasks are completed before termination.
     */
    @Override
    public void stop() {
        partitionExecutors.values().forEach(executor -> {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });
        LOG.info("PartitionKeyAwareVirtualExecutorStrategy stopped");
    }

    /**
     * Determines the partition key for a given Kafka record.
     * Ensures that records with the same partition key are processed in order by the same thread.
     *
     * @param record The Kafka {@link ConsumerRecord} to extract the partition key from.
     * @param record The Kafka consumer record.
     * @return A hash-based partition key used to select the appropriate executor.
     * <p>
     * If you want to customize your partition key, you should override the method
     */
    protected int getPartitionKey(ConsumerRecord<String, ?> record) {
        final Object message = record.value();

        if (message == null) {
            return record.partition();
        }

        if (message instanceof PartitionKeyAware) {
            return ((PartitionKeyAware) message).getPartitionKey().hashCode();
        }

        if (record.key() != null) {
            return record.key().hashCode();
        }

        return message.hashCode();
    }

}
