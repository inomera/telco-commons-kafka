package com.inomera.telco.commons.springkafka.consumer.invoker.virtual;

import com.inomera.telco.commons.springkafka.consumer.executor.virtual.VirtualExecutorStrategy;
import com.inomera.telco.commons.springkafka.consumer.invoker.ConsumerInvoker;
import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;
import com.inomera.telco.commons.springkafka.consumer.invoker.MethodInvoker;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * @author Turgay Can
 */
@RequiredArgsConstructor
public class VirtualSimpleConsumerInvoker implements ConsumerInvoker {
    private final MethodInvoker methodInvoker;
    private final VirtualExecutorStrategy executorStrategy;

    @Override
    public void start() {
        executorStrategy.start();
    }

    @Override
    public void stop() {
        executorStrategy.stop();
    }

    @Override
    public Future<InvokerResult> invoke(ConsumerRecord<String, ?> record) {
        final FutureTask<InvokerResult> futureTask = methodInvoker.addRecord(record);
        final ExecutorService executor = executorStrategy.getExecutor(record);
        executor.submit(futureTask);
        return futureTask;
    }
}
