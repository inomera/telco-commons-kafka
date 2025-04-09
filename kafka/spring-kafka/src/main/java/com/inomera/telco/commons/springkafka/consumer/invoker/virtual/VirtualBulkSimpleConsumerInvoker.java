package com.inomera.telco.commons.springkafka.consumer.invoker.virtual;

import com.inomera.telco.commons.springkafka.consumer.executor.virtual.VirtualExecutorStrategy;
import com.inomera.telco.commons.springkafka.consumer.invoker.BulkConsumerInvoker;
import com.inomera.telco.commons.springkafka.consumer.invoker.BulkInvokerResult;
import com.inomera.telco.commons.springkafka.consumer.invoker.MethodInvoker;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

@RequiredArgsConstructor
public class VirtualBulkSimpleConsumerInvoker implements BulkConsumerInvoker {
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
    public Future<BulkInvokerResult> invoke(Set<ConsumerRecord<String, ?>> records) {
        final FutureTask<BulkInvokerResult> futureTask = methodInvoker.addRecords(records);
        final ConsumerRecord<String, ?> firstRecord = records.iterator().next();
        final ExecutorService executorService = executorStrategy.getExecutor(firstRecord);
        executorService.submit(futureTask);
        return futureTask;
    }
}
