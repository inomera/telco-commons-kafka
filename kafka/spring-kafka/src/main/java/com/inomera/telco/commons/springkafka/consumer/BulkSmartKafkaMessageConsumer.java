package com.inomera.telco.commons.springkafka.consumer;

import com.inomera.telco.commons.springkafka.consumer.invoker.BulkConsumerInvoker;
import com.inomera.telco.commons.springkafka.consumer.poller.BulkConsumerPoller;
import com.inomera.telco.commons.springkafka.consumer.retry.InMemoryBulkRecordRetryConsumer;
import org.springframework.core.Ordered;

import java.util.concurrent.atomic.AtomicBoolean;


public class BulkSmartKafkaMessageConsumer implements KafkaMessageConsumer {
    private final BulkConsumerPoller consumerPoller;
    private final BulkConsumerInvoker consumerInvoker;
    private final InMemoryBulkRecordRetryConsumer inMemoryBulkRecordRetryConsumer;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public BulkSmartKafkaMessageConsumer(BulkConsumerPoller consumerPoller, BulkConsumerInvoker consumerInvoker, InMemoryBulkRecordRetryConsumer inMemoryBulkRecordRetryConsumer) {
        this.consumerPoller = consumerPoller;
        this.consumerInvoker = consumerInvoker;
        consumerPoller.setConsumerRecordHandler(consumerInvoker::invoke);
        this.inMemoryBulkRecordRetryConsumer = inMemoryBulkRecordRetryConsumer;
    }

    @Override
    public synchronized void start() {
        if (running.get()) {
            return;
        }
        running.set(true);
        consumerInvoker.start();
        consumerPoller.start();
        inMemoryBulkRecordRetryConsumer.start();
    }

    @Override
    public synchronized void stop() {
        if (!running.get()) {
            return;
        }
        consumerPoller.stop();
        consumerInvoker.stop();
        inMemoryBulkRecordRetryConsumer.stop();
        running.set(false);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public int getPhase() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
