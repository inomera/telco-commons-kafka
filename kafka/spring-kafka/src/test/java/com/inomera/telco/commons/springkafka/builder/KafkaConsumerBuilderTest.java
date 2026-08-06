package com.inomera.telco.commons.springkafka.builder;

import com.inomera.telco.commons.springkafka.consumer.KafkaMessageConsumer;
import com.inomera.telco.commons.springkafka.consumer.OffsetCommitStrategy;
import com.inomera.telco.commons.springkafka.consumer.invoker.ListenerMethodRegistry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Builder wiring that is easy to get wrong at the call site.
 */
class KafkaConsumerBuilderTest {

    private static Properties classicProperties() {
        final Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        // The three settings the client refuses alongside group.protocol=consumer. They are
        // everywhere in existing property files, which is why the library strips them.
        properties.setProperty("session.timeout.ms", "45000");
        properties.setProperty("heartbeat.interval.ms", "3000");
        properties.setProperty("partition.assignment.strategy",
                "org.apache.kafka.clients.consumer.CooperativeStickyAssignor");
        return properties;
    }

    private static KafkaConsumerBuilder builder(Properties properties, boolean rebalanceFirst) {
        final KafkaConsumerBuilder builder = KafkaConsumerBuilder.builder(new ListenerMethodRegistry())
                .groupId("builder-test-group")
                .topics("builder-test-topic")
                .offsetCommitStrategy(OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE)
                .valueDeserializer(new StringDeserializer());
        // The order under test: requesting broker-side rebalancing before handing over the
        // properties used to lose the request entirely, because properties() replaces the
        // whole Properties instance.
        return rebalanceFirst
                ? builder.serverSideRebalance("uniform").properties(properties)
                : builder.properties(properties).serverSideRebalance("uniform");
    }

    @Test
    @DisplayName("serverSideRebalance() survives a later properties() call")
    void serverSideRebalanceIsOrderIndependent() {
        for (boolean rebalanceFirst : new boolean[]{true, false}) {
            final Properties properties = classicProperties();
            final KafkaMessageConsumer consumer = builder(properties, rebalanceFirst).build();
            assertNotNull(consumer);

            assertEquals("consumer", properties.getProperty("group.protocol"),
                    "group.protocol lost when rebalanceFirst=" + rebalanceFirst);
            assertEquals("uniform", properties.getProperty("group.remote.assignor"));

            // Left in place, these fail consumer construction with a ConfigException.
            assertFalse(properties.containsKey("session.timeout.ms"));
            assertFalse(properties.containsKey("heartbeat.interval.ms"));
            assertFalse(properties.containsKey("partition.assignment.strategy"));
        }
    }

    @Test
    @DisplayName("without serverSideRebalance() the classic settings are left untouched")
    void classicProtocolIsNotTouched() {
        final Properties properties = classicProperties();
        KafkaConsumerBuilder.builder(new ListenerMethodRegistry())
                .groupId("builder-test-group")
                .topics("builder-test-topic")
                .offsetCommitStrategy(OffsetCommitStrategy.AT_LEAST_ONCE_SINGLE)
                .valueDeserializer(new StringDeserializer())
                .properties(properties)
                .build();

        assertFalse(properties.containsKey("group.protocol"));
        assertEquals("45000", properties.getProperty("session.timeout.ms"));
    }
}
