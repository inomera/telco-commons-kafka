package com.inomera.telco.commons.example.springkafka;

import com.google.protobuf.GeneratedMessageV3;
import com.inomera.echo.domain.player.command.PlayerCreateCommandProto;
import com.inomera.echo.domain.player.event.PlayerNotificationEventProto;
import com.inomera.telco.commons.example.springkafka.util.ThreadUtils;
import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.inomera.echo.domain.KafkaTopicConstants.TOPIC_PLAYER_CREATE_COMMAND;
import static com.inomera.echo.domain.KafkaTopicConstants.TOPIC_PLAYER_NOTIFICATION_EVENT;
import static com.inomera.telco.commons.springkafka.annotation.KafkaListener.RETRY.RETRY_IN_MEMORY_TASK;

/**
 * @author Serdar Kuzucu
 */
@Slf4j
@Component
public class SomeListener {

    @KafkaListener(groupId = "event-logger", topics = {TOPIC_PLAYER_CREATE_COMMAND, TOPIC_PLAYER_NOTIFICATION_EVENT}, includeSubclasses = true, retry = RETRY_IN_MEMORY_TASK, retryCount = 5, retryBackoffTime = 5000L)
    public void handle(GeneratedMessageV3 message) {
        LOG.info("handle : message={}", message);
        ThreadUtils.sleepQuietly(300);
        if (message instanceof PlayerCreateCommandProto) {
            final PlayerCreateCommandProto msg = (PlayerCreateCommandProto) message;
            if (msg.getTxKey().length() % 2 == 0) {
                LOG.warn("Commit key={}, msg={}", msg.getTxKey(), msg);
                return;
            }
            throw new RuntimeException("retry test single message consumer without retry");
        }
        if (message instanceof PlayerNotificationEventProto) {
            final PlayerNotificationEventProto msg = (PlayerNotificationEventProto) message;
            LOG.info("msg : {}", msg);
        }
    }

}
