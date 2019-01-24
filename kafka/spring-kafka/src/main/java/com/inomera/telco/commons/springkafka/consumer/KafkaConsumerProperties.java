package com.inomera.telco.commons.springkafka.consumer;

import com.inomera.telco.commons.lang.Assert;
import lombok.Getter;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * @author Serdar Kuzucu
 */
@Getter
public class KafkaConsumerProperties {
    private final String groupId;
    private final List<String> topics;
    private final Pattern topicPattern;
    private final OffsetCommitStrategy offsetCommitStrategy;
    private final Properties kafkaConsumerProperties;

    public KafkaConsumerProperties(String groupId, List<String> topics, Pattern topicPattern,
                                   OffsetCommitStrategy offsetCommitStrategy, Properties kafkaConsumerProperties) {
        Assert.hasText(groupId, "groupId is mandatory!");
        Assert.notNull(offsetCommitStrategy, "offsetCommitStrategy is mandatory!");
        Assert.notNull(kafkaConsumerProperties, "kafkaConsumerProperties is mandatory!");
        this.groupId = groupId;
        this.topics = topics;
        this.topicPattern = topicPattern;
        this.offsetCommitStrategy = offsetCommitStrategy;
        this.kafkaConsumerProperties = kafkaConsumerProperties;
    }

    public KafkaConsumerProperties(String groupId, List<String> topics, OffsetCommitStrategy offsetCommitStrategy,
                                   Properties kafkaConsumerProperties) {
        this(groupId, topics, null, offsetCommitStrategy, kafkaConsumerProperties);
        Assert.notEmpty(topics, "topics cannot be empty!");
    }

    public KafkaConsumerProperties(String groupId, Pattern topicPattern, OffsetCommitStrategy offsetCommitStrategy,
                                   Properties kafkaConsumerProperties) {
        this(groupId, null, topicPattern, offsetCommitStrategy, kafkaConsumerProperties);
        Assert.notNull(topicPattern, "topicPattern is mandatory!");
    }

    public boolean isAtMostOnceBulk() {
        return OffsetCommitStrategy.AT_MOST_ONCE_BULK == offsetCommitStrategy;
    }

    public boolean isAtMostOnceSingle() {
        return OffsetCommitStrategy.AT_MOST_ONCE_SINGLE == offsetCommitStrategy;
    }

    public boolean isAtLeastOnceSingle() {
        return OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE == offsetCommitStrategy;
    }

    public boolean isAtLeastOnceBulk() {
        return OffsetCommitStrategy.AT_LEAST_ONCE_BULK == offsetCommitStrategy;
    }

    public boolean isAtLeastOnce() {
        return isAtLeastOnceBulk() || isAtLeastOnceSingle();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("KafkaConsumerProperties [");
        sb.append("groupId=").append(groupId);
        sb.append(", topics=").append(topics);
        sb.append(", topicPattern=").append(topicPattern);
        sb.append("]");
        return sb.toString();
    }

    public String getClientId() {
        return "Consumer_" + getContainerId();
    }

    public boolean hasPatternBasedTopic() {
        return topicPattern != null;
    }

    private StringBuilder getContainerId() {
        final StringBuilder sbContainerId = new StringBuilder();
        sbContainerId.append(groupId).append('-');
        sbContainerId.append(getNodeName());
        return sbContainerId;
    }

    private String getNodeName() {
        String serverName = null;
        try {
            serverName = ManagementFactory.getRuntimeMXBean().getName();
        } catch (Exception ignored) {
        }

        if (serverName == null) {
            throw new RuntimeException("Can not determine server instance name.");
        }

        return serverName.replace('@', '-');
    }
}
