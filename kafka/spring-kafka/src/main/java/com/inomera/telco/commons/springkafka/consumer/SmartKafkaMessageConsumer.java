package com.inomera.telco.commons.springkafka.consumer;

import com.inomera.telco.commons.springkafka.consumer.invoker.ConsumerInvoker;
import com.inomera.telco.commons.springkafka.consumer.poller.ConsumerPoller;
import com.inomera.telco.commons.springkafka.consumer.retry.InMemoryRecordRetryConsumer;
import org.springframework.core.Ordered;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Serdar Kuzucu
 * @author Turgay Can
 */
public class SmartKafkaMessageConsumer implements KafkaMessageConsumer {
    private final ConsumerPoller consumerPoller;
    private final ConsumerInvoker consumerInvoker;
    private final InMemoryRecordRetryConsumer inMemoryRecordRetryConsumer;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public SmartKafkaMessageConsumer(ConsumerPoller consumerPoller, ConsumerInvoker consumerInvoker, InMemoryRecordRetryConsumer inMemoryRecordRetryConsumer) {
        this.consumerPoller = consumerPoller;
        this.consumerInvoker = consumerInvoker;
        consumerPoller.setConsumerRecordHandler(consumerInvoker::invoke);
        this.inMemoryRecordRetryConsumer = inMemoryRecordRetryConsumer;
    }

    @Override
    public synchronized void start() {
        if (running.get()) {
            return;
        }
        running.set(true);
        consumerInvoker.start();
        consumerPoller.start();
        inMemoryRecordRetryConsumer.start();
    }

    @Override
    public synchronized void stop() {
        if (!running.get()) {
            return;
        }
        consumerPoller.stop();
        consumerInvoker.stop();
        inMemoryRecordRetryConsumer.stop();
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
