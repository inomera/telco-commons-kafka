package com.inomera.telco.commons.example.domain.constant;

import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import com.google.protobuf.GeneratedMessage;
import player.command.PlayerCreateCommandProto;
import player.event.PlayerNotificationEventProto;
import todo.command.TodoUpdateCommandProto;
import todo.event.TodoInfoRequestEventProto;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KafkaTopicConstants {
    public static final String TOPIC_PLAYER_CREATE_COMMAND = "echo.cmd.PlayerCreateCommand";
    public static final String TOPIC_PLAYER_NOTIFICATION_EVENT = "echo.evt.PlayerNotificationEvent";
    public static final String TOPIC_TODO_UPDATE_COMMAND = "echo.cmd.PlayerCreateCommand";
    public static final String TOPIC_TODO_INFO_REQUEST_EVENT = "echo.evt.TodoInfoRequestEvent";

    // Put all event/command classes (classes that will be published to kafka)
    // into this map and match them with their topic names
    public static final Map<Class<? extends GeneratedMessage>, String> CLASS_TOPIC_MAP =
            ImmutableMap.<Class<? extends GeneratedMessage>, String>builder()
                    // Example classes will be removed in the future.
                    .put(PlayerCreateCommandProto.class, TOPIC_PLAYER_CREATE_COMMAND)
                    .put(PlayerNotificationEventProto.class, TOPIC_PLAYER_NOTIFICATION_EVENT)
                    .put(TodoUpdateCommandProto.class, TOPIC_TODO_UPDATE_COMMAND)
                    .put(TodoInfoRequestEventProto.class, TOPIC_TODO_INFO_REQUEST_EVENT)
                    .build();
}
