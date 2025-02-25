package com.inomera.telco.commons.springkafka.consumer.executor;

import com.inomera.telco.commons.springkafka.PartitionKeyAware;
import com.inomera.telco.commons.springkafka.consumer.executor.virtual.PartitionKeyAwareVirtualExecutorStrategy;
import com.inomera.telco.commons.springkafka.util.ThreadPoolExecutorUtils;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Execution strategy that assigns Kafka consumer records to dedicated thread pools based on partition keys.
 * <p>
 * This strategy ensures that records with the same partition key are always processed by the same thread,
 * preserving data consistency and minimizing concurrency issues.
 * </p>
 *
 * <h3>Usage Guidelines:</h3>
 * <ul>
 *     <li>Use {@link PartitionKeyAwareVirtualExecutorStrategy} for **IO-bound** scenarios (DB queries, API calls, messaging) → Virtual Threads.</li>
 *     <li>Use {@link PartitionKeyAwareExecutorStrategy} for **CPU-bound** scenarios (intensive computations, multi-threaded processing) → OS Threads.</li>
 * </ul>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *     <li>Ensures affinity-based processing of Kafka records by mapping partition keys to worker threads.</li>
 *     <li>Supports a configurable number of worker threads, each handling a dedicated partition group.</li>
 *     <li>Prevents contention and concurrency issues when processing records with shared partition keys.</li>
 * </ul>
 *
 * @author Serdar Kuzucu
 * @author Turgay Can
 */
@RequiredArgsConstructor
public class PartitionKeyAwareExecutorStrategy implements ExecutorStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(PartitionKeyAwareExecutorStrategy.class);

    /**
     * Number of dedicated worker threads to handle Kafka records.
     */
    private final int numberOfInvokerThreads;

    /**
     * Factory to create worker threads.
     */
    private final ThreadFactory invokerThreadFactory;

    /**
     * Array of thread pool executors assigned to partitioned workloads.
     */
    private ThreadPoolExecutor[] executors;

    /**
     * Retrieves the executor service responsible for handling the given Kafka record.
     * Ensures that all records with the same partition key are processed by the same executor.
     *
     * @param record The Kafka consumer record.
     * @return The assigned {@link ThreadPoolExecutor} for the record's partition.
     */
    @Override
    public ThreadPoolExecutor get(ConsumerRecord<String, ?> record) {
        final int partitionKey = getPartitionKey(record);
        final int workerIndex = Math.abs(partitionKey) % executors.length;
        return executors[workerIndex];
    }

    /**
     * Initializes the thread pool executors, creating a fixed number of worker threads.
     */
    @Override
    public void start() {
        executors = new ThreadPoolExecutor[numberOfInvokerThreads];
        for (int i = 0; i < numberOfInvokerThreads; i++) {
            executors[i] = createExecutor();
        }
    }

    /**
     * Gracefully shuts down all thread pool executors, ensuring that all pending tasks are completed.
     */
    @Override
    public void stop() {
        if (executors != null) {
            for (ThreadPoolExecutor executor : executors) {
                ThreadPoolExecutorUtils.closeGracefully(executor, LOG, "PartitionKeyAwareExecutorStrategy");
            }
        }
    }

    /**
     * Creates a single-threaded executor service for processing partitioned Kafka records.
     *
     * @return A new {@link ThreadPoolExecutor} instance.
     */
    private ThreadPoolExecutor createExecutor() {
        return new ThreadPoolExecutor(0, 1, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>(), invokerThreadFactory);
    }

    /**
     * Extracts the partition key from the given Kafka consumer record.
     * <p>
     * The partition key is determined based on the following priority order:
     * <ul>
     *     <li>If the message implements {@link PartitionKeyAware}, the partition key from the message is used.</li>
     *     <li>If the record has a non-null key, its hash code is used as the partition key.</li>
     *     <li>Otherwise, the hash code of the message value is used.</li>
     * </ul>
     * </p>
     * If you want to customize your partition key, you should override the method
     *
     * @param record The Kafka consumer record.
     * @return The computed partition key.
     */
    protected int getPartitionKey(ConsumerRecord<String, ?> record) {
        final Object message = record.value();

        if (message == null) {
            return 0;
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
