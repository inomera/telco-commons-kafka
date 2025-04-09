package com.inomera.telco.commons.springkafka.consumer.invoker;

import com.inomera.telco.commons.springkafka.consumer.executor.ExecutorStrategy;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

@RequiredArgsConstructor
public class BulkSimpleConsumerInvoker implements BulkConsumerInvoker {
    private final MethodInvoker methodInvoker;
    private final ExecutorStrategy executorStrategy;

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
        final ThreadPoolExecutor executor = executorStrategy.get(firstRecord);
        executor.submit(futureTask);
        return futureTask;
    }
}
