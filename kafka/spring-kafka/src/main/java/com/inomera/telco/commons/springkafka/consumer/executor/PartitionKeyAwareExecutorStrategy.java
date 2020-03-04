package com.inomera.telco.commons.springkafka.consumer.executor;

import com.inomera.telco.commons.springkafka.PartitionKeyAware;
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
 * @author Serdar Kuzucu
 */
@RequiredArgsConstructor
public class PartitionKeyAwareExecutorStrategy implements ExecutorStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(PartitionKeyAwareExecutorStrategy.class);

    private final int numberOfInvokerThreads;
    private final ThreadFactory invokerThreadFactory;
    private ThreadPoolExecutor[] executors;

    @Override
    public ThreadPoolExecutor get(ConsumerRecord<String, ?> record) {
        final int partitionKey = getPartitionKey(record);
        final int workerIndex = Math.abs(partitionKey) % executors.length;
        return executors[workerIndex];
    }

    @Override
    public void start() {
        executors = new ThreadPoolExecutor[numberOfInvokerThreads];
        for (int i = 0; i < numberOfInvokerThreads; i++) {
            executors[i] = createExecutor();
        }
    }

    @Override
    public void stop() {
        if (executors != null) {
            for (ThreadPoolExecutor executor : executors) {
                ThreadPoolExecutorUtils.closeGracefully(executor, LOG, "PartitionKeyAwareExecutorStrategy");
            }
        }
    }

    private ThreadPoolExecutor createExecutor() {
        return new ThreadPoolExecutor(0, 1, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>(), invokerThreadFactory);
    }

    private int getPartitionKey(ConsumerRecord<String, ?> record) {
        final Object message = record.value();

        if (message instanceof PartitionKeyAware) {
            return ((PartitionKeyAware) message).getPartitionKey().hashCode();
        }

        if (record.key() != null) {
            return record.key().hashCode();
        }

        return message.hashCode();
    }
}
