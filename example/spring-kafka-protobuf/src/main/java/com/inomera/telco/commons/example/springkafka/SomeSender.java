package com.inomera.telco.commons.example.springkafka;

import com.inomera.echo.domain.player.command.PlayerCreateCommandProto;
import com.inomera.echo.domain.player.event.PlayerNotificationEventProto;
import com.inomera.echo.domain.todo.TodoRequestProto;
import com.inomera.echo.domain.todo.event.TodoInfoRequestEventProto;
import com.inomera.telco.commons.example.springkafka.util.ThreadUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class SomeSender {
    private static final Logger LOG = LoggerFactory.getLogger(SomeSender.class);

    private final EventPublisher eventPublisher;
    public final AtomicInteger atomicInteger = new AtomicInteger(1);

    @Scheduled(fixedDelay = 1000)
    public void publishRandomText() {
        LOG.info("Sending event");
        for (int i = 0; i < 4; i++) {
            ThreadUtils.sleepQuietly(3000);
            final int value = atomicInteger.incrementAndGet();
            if (value % 3 == 0) {
                eventPublisher.fire(PlayerNotificationEventProto.newBuilder()
                        .setId(RandomUtils.nextLong())
                        .setName(value + "-yattara")
                        .setStatus("active")
                        .setLogTrackKey(TransactionKeyUtils.generateTxKey())
                        .build());
                continue;
            }
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
                    .setId(RandomUtils.nextLong())
                    .setInfo(value + "-todo-req")
                    .setName(value + "-todo-req")
                    .build());
        }
        LOG.info("Sent event");
    }
}
