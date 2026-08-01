package com.inomera.telco.commons.example.springkafka.e2e;

import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import todo.command.TodoUpdateCommandProto;

/**
 * Always fails, so the in-memory retry path can be observed live.
 *
 * <p>Watch the log for the spacing between "E2E retry attempt" lines: before 4.1.0
 * {@code retryBackoffTime} was only consulted in the exit condition, so every attempt landed
 * within microseconds and the retry budget was burned instantly. Attempts should now be spread
 * out, and after {@code retryCount} the record is handed to the dead-letter handler
 * ("Retries exhausted after N attempts").
 */
@RequiredArgsConstructor
public class E2eRetryListener {
    private static final Logger LOG = LoggerFactory.getLogger(E2eRetryListener.class);

    public static final String GROUP = "e2e-retry-logger";

    /**
     * A topic of its own. {@code KafkaTopicConstants.TOPIC_TODO_UPDATE_COMMAND} is not usable
     * here: it is assigned the same string as {@code TOPIC_PLAYER_CREATE_COMMAND}
     * ({@code echo.cmd.PlayerCreateCommand}), so the scenario would land on the topic the
     * regular listeners already consume.
     */
    public static final String TOPIC = "echo.e2e.retry";

    private final E2eMetrics metrics;

    @KafkaListener(groupId = GROUP, topics = TOPIC,
            retry = KafkaListener.RETRY.RETRY_IN_MEMORY_TASK, retryCount = 3, retryBackoffTime = 500L)
    public void alwaysFails(TodoUpdateCommandProto message) {
        final long attempt = metrics.recordRetryAttempt();
        LOG.warn("E2E retry attempt #{} for txKey={} — throwing on purpose", attempt, message.getTxKey());
        throw new IllegalStateException("E2E scenario: deliberate failure for " + message.getTxKey());
    }
}
