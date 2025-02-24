package com.inomera.telco.commons.springkafka.consumer.invoker.virtual;

import com.inomera.telco.commons.springkafka.consumer.executor.virtual.VirtualExecutorStrategy;
import com.inomera.telco.commons.springkafka.consumer.invoker.ConsumerInvoker;
import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;
import com.inomera.telco.commons.springkafka.consumer.invoker.MethodInvoker;
import com.inomera.telco.commons.springkafka.consumer.invoker.RejectionHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.concurrent.*;

/**
 * @author Turgay Can
 */
@Slf4j
public class VirtualRejectionAwareConsumerInvoker implements ConsumerInvoker {
    private final MethodInvoker methodInvoker;
    private final VirtualExecutorStrategy executorStrategy;
    private final RejectionHandler rejectionHandler;

    public VirtualRejectionAwareConsumerInvoker(MethodInvoker methodInvoker, VirtualExecutorStrategy executorStrategy, RejectionHandler rejectionHandler) {
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
            final ExecutorService executorService = executorStrategy.getExecutor(record);
            executorService.submit(futureTask);
        } catch (RejectedExecutionException e) {
            rejectionHandler.handleReject(record, futureTask);
        }
        return futureTask;
    }
}
