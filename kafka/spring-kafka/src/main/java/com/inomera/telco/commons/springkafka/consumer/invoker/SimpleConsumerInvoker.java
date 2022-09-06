package com.inomera.telco.commons.springkafka.consumer.invoker;

import com.inomera.telco.commons.springkafka.consumer.executor.ExecutorStrategy;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Serdar Kuzucu
 */
@RequiredArgsConstructor
public class SimpleConsumerInvoker implements ConsumerInvoker {
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
    public Future<InvokerResult> invoke(ConsumerRecord<String, ?> record) {
        final FutureTask<InvokerResult> futureTask = methodInvoker.addRecord(record);
        final ThreadPoolExecutor executor = executorStrategy.get(record);
        executor.submit(futureTask);
        return futureTask;
    }
}
