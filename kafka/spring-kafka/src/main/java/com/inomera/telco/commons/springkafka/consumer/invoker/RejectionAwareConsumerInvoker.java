package com.inomera.telco.commons.springkafka.consumer.invoker;

import com.inomera.telco.commons.springkafka.consumer.executor.ExecutorStrategy;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Serdar Kuzucu
 */
public class RejectionAwareConsumerInvoker implements ConsumerInvoker {
    private final MethodInvoker methodInvoker;
    private final ExecutorStrategy executorStrategy;
    private final RejectionHandler rejectionHandler;

    public RejectionAwareConsumerInvoker(MethodInvoker methodInvoker, ExecutorStrategy executorStrategy, RejectionHandler rejectionHandler) {
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
    public Future<InvokerResult> invoke(ConsumerRecord<String, ?> record) {
        final FutureTask<InvokerResult> futureTask = methodInvoker.addRecord(record);
        try {
            final ThreadPoolExecutor executor = executorStrategy.get(record);
            executor.submit(futureTask);
        } catch (RejectedExecutionException e) {
            rejectionHandler.handleReject(record, futureTask);
        }
        return futureTask;
    }
}
