package com.inomera.telco.commons.example.springkafka;


import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import player.command.PlayerCreateCommandProto;

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

    @Scheduled(fixedDelay = 5000)
    public void publishRandomText() {
        if (true || running.get()) {
            LOG.trace("do not send max limit reached");
            return;
        }
        if (atomicInteger.getAndIncrement() == 10) {
            running.set(true);
            LOG.debug("Senttttt");
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LOG.info("Sending event");
        for (int i = 0; i < 1_000_000; i++) {
//            ThreadUtils.sleepQuietly(3000);
//            final int value = atomicInteger.incrementAndGet();
//            if (i % 2 == 0) {
//                eventPublisher.fire(PlayerNotificationEventProto.newBuilder()
//                        .setId(RandomUtils.secure().randomLong())
//                        .setName(i + "-yattara")
//                        .setStatus("active")
//                        .setLogTrackKey(TransactionKeyUtils.generateTxKey())
//                        .build());
//            continue;
//            }
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
