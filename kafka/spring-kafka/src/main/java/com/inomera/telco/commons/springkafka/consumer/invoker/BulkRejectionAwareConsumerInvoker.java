package com.inomera.telco.commons.springkafka.consumer.invoker;

import com.inomera.telco.commons.springkafka.consumer.executor.ExecutorStrategy;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;


public class BulkRejectionAwareConsumerInvoker implements BulkConsumerInvoker {
    private final MethodInvoker methodInvoker;
    private final ExecutorStrategy executorStrategy;
    private final RejectionHandler rejectionHandler;

    public BulkRejectionAwareConsumerInvoker(MethodInvoker methodInvoker, ExecutorStrategy executorStrategy, RejectionHandler rejectionHandler) {
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
    public Future<InvokerResult> invoke(Set<ConsumerRecord<String, ?>> records) {
        final FutureTask<InvokerResult> futureTask = methodInvoker.addRecords(records);
        try {
            final ConsumerRecord<String, ?> firstRecord = records.iterator().next();
            final ThreadPoolExecutor executor = executorStrategy.get(firstRecord);
            executor.submit(futureTask);
        } catch (RejectedExecutionException e) {
            rejectionHandler.handleReject(records, futureTask);
        }
        return futureTask;
    }
}
