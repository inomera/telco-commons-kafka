package com.inomera.telco.commons.example.springkafka;

import com.google.protobuf.GeneratedMessage;
import com.inomera.telco.commons.example.springkafka.util.ThreadUtils;
import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import player.command.PlayerCreateCommandProto;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.inomera.telco.commons.example.domain.constant.KafkaTopicConstants.*;
import static com.inomera.telco.commons.example.springkafka.SpringKafkaProtobufExampleApplication.EVENT_LOGGER;
import static com.inomera.telco.commons.example.springkafka.SpringKafkaProtobufExampleApplication.VIRTUAL_EVENT_LOGGER;

/**
 * @author Serdar Kuzucu
 */
@Slf4j
@Component
public class SomeListener {
    static final AtomicInteger counter = new AtomicInteger(0);
    static final AtomicInteger vCounter = new AtomicInteger(0);
    static ConcurrentHashMap<String, Long> msgHolder = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, Long> vMsgHolder = new ConcurrentHashMap<>();

    @KafkaListener(groupId = EVENT_LOGGER, topics = {TOPIC_PLAYER_CREATE_COMMAND})
    public void handle(PlayerCreateCommandProto messages) {
        msgHolder.putIfAbsent("firstTime", Instant.now().toEpochMilli());

        ThreadUtils.sleepQuietly(100);
        int count = counter.incrementAndGet();
        if (count % 1_000_000 == 0) {
            LOG.info("OS THREAD!! count: {}, firstTime: {}, last : {}, lastTime={}, duration : {}", count, msgHolder.get("firstTime"), messages, Instant.now().toEpochMilli(),
                    Duration.ofMillis(Instant.now().toEpochMilli() - msgHolder.get("firstTime")));
        }
    }

    @KafkaListener(groupId = VIRTUAL_EVENT_LOGGER, topics = {TOPIC_PLAYER_CREATE_COMMAND, TOPIC_MESSAGES_EVENT}, includeSubclasses = true)
    public void handleVirtual(GeneratedMessage message) {
        vMsgHolder.putIfAbsent("firstTime", Instant.now().toEpochMilli());

        ThreadUtils.sleepQuietly(100);
        int count = vCounter.incrementAndGet();
        LOG.info("VIRTUAL THREAD!! count: {}, firstTime: {}, message : {}, lastTime={}, duration : {}", count, vMsgHolder.get("firstTime"), message, Instant.now().toEpochMilli(),
                Duration.ofMillis(Instant.now().toEpochMilli() - vMsgHolder.get("firstTime")));
//        if (count % 1_000_000 == 0) {
//            LOG.info("VIRTUAL THREAD!! count: {}, firstTime: {}, last : {}, lastTime={}, duration : {}", count, vMsgHolder.get("firstTime"), messages, Instant.now().toEpochMilli(),
//                    Duration.ofMillis(Instant.now().toEpochMilli() - vMsgHolder.get("firstTime")));
//        }
    }

    @KafkaListener(groupId = "event-logger", topics = {TOPIC_PLAYER_NOTIFICATION_EVENT, TOPIC_PLAYER_CREATE_COMMAND}, includeSubclasses = true)
    public void handle(Set<GeneratedMessage> messages) {
        msgHolder.putIfAbsent("firstTime", Instant.now().toEpochMilli());

        ThreadUtils.sleepQuietly(100);
        LOG.info("firstTime: {}, last : {}, lastTime={}, duration : {}", msgHolder.get("firstTime"), messages, Instant.now().toEpochMilli(),
                Duration.ofMillis(Instant.now().toEpochMilli() - msgHolder.get("firstTime")));
//        if (message instanceof PlayerCreateCommandProto) {
//            final PlayerCreateCommandProto msg = (PlayerCreateCommandProto) message;
//            if (msg.getTxKey().length() % 2 == 0) {
//                LOG.warn("Commit key={}, msg={}", msg.getTxKey(), msg);
//                return;
//            }
//            throw new RuntimeException("retry test single message consumer without retry");
//        }
//        if (message instanceof PlayerNotificationEventProto) {
//            final PlayerNotificationEventProto msg = (PlayerNotificationEventProto) message;
//            LOG.info("msg : {}", msg);
//        }
    }

}
