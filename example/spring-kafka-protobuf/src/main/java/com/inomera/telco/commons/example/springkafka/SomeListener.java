package com.inomera.telco.commons.example.springkafka;

import com.google.protobuf.GeneratedMessage;
import com.inomera.telco.commons.example.springkafka.util.ThreadUtils;
import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.inomera.telco.commons.example.domain.constant.KafkaTopicConstants.TOPIC_PLAYER_CREATE_COMMAND;
import static com.inomera.telco.commons.example.domain.constant.KafkaTopicConstants.TOPIC_PLAYER_NOTIFICATION_EVENT;

/**
 * @author Serdar Kuzucu
 */
@Slf4j
@Component
public class SomeListener {
    static ConcurrentHashMap<String, Long> msgHolder = new ConcurrentHashMap<>();

    @KafkaListener(groupId = "event-logger", topics = {TOPIC_PLAYER_CREATE_COMMAND, TOPIC_PLAYER_NOTIFICATION_EVENT}, includeSubclasses = true)
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
