package com.inomera.telco.commons.springkafka.consumer.invoker.virtual;

import com.inomera.telco.commons.springkafka.consumer.executor.virtual.VirtualExecutorStrategy;
import com.inomera.telco.commons.springkafka.consumer.invoker.BulkConsumerInvoker;
import com.inomera.telco.commons.springkafka.consumer.invoker.BulkInvokerResult;
import com.inomera.telco.commons.springkafka.consumer.invoker.MethodInvoker;
import com.inomera.telco.commons.springkafka.consumer.invoker.RejectionHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Set;
import java.util.concurrent.*;


public class VirtualBulkRejectionAwareConsumerInvoker implements BulkConsumerInvoker {
    private final MethodInvoker methodInvoker;
    private final VirtualExecutorStrategy executorStrategy;
    private final RejectionHandler rejectionHandler;

    public VirtualBulkRejectionAwareConsumerInvoker(MethodInvoker methodInvoker, VirtualExecutorStrategy executorStrategy, RejectionHandler rejectionHandler) {
        this.methodInvoker = methodInvoker;
        this.executorStrategy = executorStrategy;
        this.rejectionHandler = rejectionHandler;
    }

    @Override
    public void start() {
        rejectionHandler.start();
        executorStrategy.start();
    }

    @Override
    public void stop() {
        rejectionHandler.stop();
        executorStrategy.stop();
    }

    @Override
    public Future<BulkInvokerResult> invoke(Set<ConsumerRecord<String, ?>> records) {
        final FutureTask<BulkInvokerResult> futureTask = methodInvoker.addRecords(records);
        try {
            final ConsumerRecord<String, ?> firstRecord = records.iterator().next();
            final ExecutorService executorService = executorStrategy.getExecutor(firstRecord);
            executorService.submit(futureTask);
        } catch (RejectedExecutionException e) {
            rejectionHandler.handleReject(records, futureTask);
        }
        return futureTask;
    }
}
