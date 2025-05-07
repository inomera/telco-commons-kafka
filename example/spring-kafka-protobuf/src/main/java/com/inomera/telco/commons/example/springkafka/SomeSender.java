package com.inomera.telco.commons.example.springkafka;


import lombok.RequiredArgsConstructor;
import messaging.OrderMessage;
import messaging.PartitionMessage;
import messaging.PaymentMessage;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import player.command.PlayerCreateCommandProto;
import player.event.PlayerNotificationEventProto;
import todo.TodoRequestProto;
import todo.event.TodoInfoRequestEventProto;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class SomeSender {
    private static final Logger LOG = LoggerFactory.getLogger(SomeSender.class);

    private final EventPublisher eventPublisher;
    private final AtomicInteger atomicInteger = new AtomicInteger(1);
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 5000)
    public void publishRandomText() {
        if (running.get() || true) {
            LOG.trace("do not send max limit reached");
            return;
        }
        if (atomicInteger.getAndIncrement() == 1) {
            running.set(false);
            LOG.debug("Senttttt");
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LOG.info("Sending event");
        int limitIndex = 3_000;
        for (int i = 0; i < limitIndex; i++) {
            final int value = atomicInteger.incrementAndGet();
            if (i % 2 == 0) {
                eventPublisher.fire(PlayerNotificationEventProto.newBuilder()
                        .setId(RandomUtils.secure().randomLong())
                        .setName(i + "-yattara")
                        .setStatus("active")
                        .setLogTrackKey(TransactionKeyUtils.generateTxKey())
                        .build());
            continue;
            }
            eventPublisher.fire(PlayerCreateCommandProto.newBuilder()
                    .setTxKey(i + "-txKey")
                    .setLogTrackKey(TransactionKeyUtils.generateTxKey())
                    .build());
            eventPublisher.fire(OrderMessage.newBuilder()
                    .setCustomerName(i + "-turgay")
                    .setOrderId(i + "-order")
                    .setPartition(PartitionMessage.newBuilder()
                            .setPartitionKey("order-key")
                            .setLogTrackKey(TransactionKeyUtils.generateTxKey())
                            .build())
                    .build());
            eventPublisher.fire(PaymentMessage.newBuilder()
                    .setPaymentId(i + "-payment")
                    .setAmount(RandomUtils.secure().randomDouble())
                    .setPartition(PartitionMessage.newBuilder()
                            .setPartitionKey("payment-key")
                            .setLogTrackKey(TransactionKeyUtils.generateTxKey())
                            .build())
                    .build());
            if (value % 3 == 1) {
                eventPublisher.fire(PlayerCreateCommandProto.newBuilder()
                        .setTxKey(value + "-txKey")
                        .setLogTrackKey(TransactionKeyUtils.generateTxKey())
                        .build());
                continue;
            }
            if (value % 3 == 2) {
                eventPublisher.fire(TodoInfoRequestEventProto.newBuilder()
                        .setInfo(value + "-todo-info")
                        .setTxKey(value + "-txKey")
                        .setLogTrackKey(TransactionKeyUtils.generateTxKey())
                        .build());
                continue;
            }
            eventPublisher.fire(TodoRequestProto.newBuilder()
                    .setId(RandomUtils.secure().randomLong())
                    .setInfo(value + "-todo-req")
                    .setName(value + "-todo-req")
                    .build());
        }
        stopWatch.stop();
        LOG.info("Sent {} events. finished seconds : {}", limitIndex, stopWatch.getTime(TimeUnit.SECONDS));
    }
}
