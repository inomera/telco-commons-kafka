package com.inomera.telco.commons.example.domain.constant;

import com.google.protobuf.GeneratedMessage;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KafkaTopicUtils {
    @SafeVarargs
    public static List<String> getTopicNames(Class<? extends GeneratedMessage>... classes) {
        if (classes == null || classes.length == 0) {
            return List.of();
        }
        return Arrays.stream(classes)
                .map(KafkaTopicUtils::getTopicName)
                .collect(Collectors.toList());
    }

    public static String getTopicName(Class<? extends GeneratedMessage> protoClass) {
        final var topicName = KafkaTopicConstants.CLASS_TOPIC_MAP.get(protoClass);
        if (topicName == null) {
            throw new IllegalArgumentException("No topic is specified for class [" + protoClass.getName() + "]. "
                    + "You need to specify topic name in " + KafkaTopicConstants.class.getName()
                    + " and create a mapping in CLASS_TOPIC_MAP constant.");
        }
        return topicName;
    }
}
