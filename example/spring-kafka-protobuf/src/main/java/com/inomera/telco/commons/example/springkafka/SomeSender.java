package com.inomera.telco.commons.example.springkafka;


import lombok.RequiredArgsConstructor;
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
    public final AtomicInteger atomicInteger = new AtomicInteger(1);
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(initialDelay = 10000, fixedDelay = 10000)
    public void publishRandomText() {
        if (running.get()) {
            LOG.debug("Senttttt");
            return;
        }
        running.set(true);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LOG.info("Sending event");
        for (int i = 0; i < 10_000; i++) {
//            ThreadUtils.sleepQuietly(3000);
//            final int value = atomicInteger.incrementAndGet();
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
//            if (value % 3 == 1) {
//                eventPublisher.fire(PlayerCreateCommandProto.newBuilder()
//                        .setTxKey(value + "-txKey")
//                        .setLogTrackKey(TransactionKeyUtils.generateTxKey())
//                        .build());
//                continue;
//            }
//            if (value % 3 == 2) {
//                eventPublisher.fire(TodoInfoRequestEventProto.newBuilder()
//                        .setInfo(value + "-todo-info")
//                        .setTxKey(value + "-txKey")
//                        .setLogTrackKey(TransactionKeyUtils.generateTxKey())
//                        .build());
//                continue;
//            }
//            eventPublisher.fire(TodoRequestProto.newBuilder()
//                    .setId(RandomUtils.secure().randomLong())
//                    .setInfo(value + "-todo-req")
//                    .setName(value + "-todo-req")
//                    .build());
        }
        stopWatch.stop();
        LOG.info("Sent 10_000 events. finished seconds : {}", stopWatch.getTime(TimeUnit.SECONDS));
    }
}
