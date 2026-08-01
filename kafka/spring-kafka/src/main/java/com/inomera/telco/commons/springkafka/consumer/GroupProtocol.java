package com.inomera.telco.commons.springkafka.consumer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

/**
 * Switches a consumer to the KIP-848 group protocol, where the <em>broker</em> computes the
 * partition assignment instead of a client-side group leader.
 *
 * <h2>Why delegate</h2>
 * With the classic protocol one member is elected leader and runs the assignor locally; every
 * membership change stops the world for the whole group while the leader recomputes and everyone
 * re-joins. Under {@code group.protocol=consumer} the group coordinator owns the assignment and
 * reconciles it incrementally: a joining or leaving member only affects the partitions that
 * actually move.
 *
 * <h2>Consequences for this library</h2>
 * Assignment is always incremental, so {@code onPartitionsAssigned} carries <em>only the newly
 * added</em> partitions and {@code onPartitionsRevoked} only the ones being taken away.
 * {@code AbstractConsumerPoller} is written for that (it never clears its in-flight state on
 * assignment and implements {@code onPartitionsLost}); the pre-4.1.0 poller was not, and died on
 * the first record after an incremental assignment.
 *
 * <h2>Incompatible settings</h2>
 * The client rejects three classic-protocol settings outright with a {@code ConfigException}:
 * {@code session.timeout.ms}, {@code heartbeat.interval.ms} and
 * {@code partition.assignment.strategy} — the broker owns those concerns now. They are common in
 * existing property files, so {@link #enable} drops them and logs what it dropped rather than
 * letting consumer construction blow up.
 *
 * @author Turgay Can
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GroupProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(GroupProtocol.class);

    static final String GROUP_PROTOCOL_CONFIG = "group.protocol";
    static final String GROUP_REMOTE_ASSIGNOR_CONFIG = "group.remote.assignor";
    static final String CONSUMER_PROTOCOL = "consumer";

    /** Settings the client refuses to accept alongside {@code group.protocol=consumer}. */
    private static final List<String> CLASSIC_ONLY_SETTINGS = List.of(
            "session.timeout.ms",
            "heartbeat.interval.ms",
            "partition.assignment.strategy");

    /**
     * Enables broker-side assignment on the given properties, in place.
     *
     * @param properties     consumer properties to modify
     * @param remoteAssignor server-side assignor name ({@code uniform} or {@code range}), or
     *                       {@code null} to let the broker choose its default
     */
    public static void enable(Properties properties, String remoteAssignor) {
        properties.setProperty(GROUP_PROTOCOL_CONFIG, CONSUMER_PROTOCOL);
        if (remoteAssignor != null && !remoteAssignor.isBlank()) {
            properties.setProperty(GROUP_REMOTE_ASSIGNOR_CONFIG, remoteAssignor);
        }

        final List<String> dropped = CLASSIC_ONLY_SETTINGS.stream()
                .filter(properties::containsKey)
                .toList();
        dropped.forEach(properties::remove);

        if (!dropped.isEmpty()) {
            LOG.warn("group.protocol=consumer delegates rebalancing to the broker, which rejects {}. "
                            + "Removed them; the broker's own timeouts apply instead.", dropped);
        }
        LOG.info("Broker-side rebalancing enabled (group.protocol=consumer, group.remote.assignor={})",
                properties.getProperty(GROUP_REMOTE_ASSIGNOR_CONFIG, "<broker default>"));
    }

    /** Whether the properties already ask for the KIP-848 protocol. */
    public static boolean isServerSide(Properties properties) {
        return CONSUMER_PROTOCOL.equalsIgnoreCase(properties.getProperty(GROUP_PROTOCOL_CONFIG, ""));
    }
}
