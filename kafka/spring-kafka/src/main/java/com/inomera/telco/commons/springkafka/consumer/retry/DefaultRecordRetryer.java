package com.inomera.telco.commons.springkafka.consumer.retry;

import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns a failed invocation into a retry decision.
 *
 * <p>Rewritten in 4.1.0. {@code RETRY_FROM_BROKER} used to throw
 * {@code RetriableCommitFailedException} from inside the poll loop, which unwound the loop and
 * killed the poller thread; recovery then depended on a thread-state checker that is disabled by
 * default, so a single poison record could take a consumer &mdash; and, after rebalancing, the
 * whole group &mdash; down. It now simply withholds the offset, which is exactly the documented
 * behaviour ("do not ack/commit; the consumer polls the message from the broker again").
 *
 * @author Turgay Can
 */
public class DefaultRecordRetryer implements RecordRetryer {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultRecordRetryer.class);

    private final InMemoryRecordRetryConsumer inMemoryRetryConsumer;

    public DefaultRecordRetryer() {
        this(null);
    }

    public DefaultRecordRetryer(InMemoryRecordRetryConsumer inMemoryRetryConsumer) {
        this.inMemoryRetryConsumer = inMemoryRetryConsumer;
    }

    @Override
    public void checkAndRetry(InvokerResult result) {
        checkAndDecide(result);
    }

    @Override
    public boolean checkAndDecide(InvokerResult result) {
        final KafkaListener kafkaListener = result.getKafkaListener();
        // A null annotation means the listener completed normally.
        if (kafkaListener == null || kafkaListener.retry() == KafkaListener.RETRY.NONE) {
            return true;
        }

        final ConsumerRecord<String, ?> record = result.getRecord();

        return switch (kafkaListener.retry()) {
            case RETRY_FROM_BROKER -> {
                LOG.warn("Holding offset back for {}-{}@{}; the broker will re-deliver it.",
                        record.topic(), record.partition(), record.offset());
                yield false;
            }
            case RETRY_IN_MEMORY_TASK -> {
                if (inMemoryRetryConsumer == null) {
                    LOG.warn("RETRY_IN_MEMORY_TASK requested for {}-{}@{} but no retry consumer is wired; "
                                    + "holding the offset back instead.",
                            record.topic(), record.partition(), record.offset());
                    yield false;
                }
                final RetryPolicy policy = RetryPolicy.fromListener(
                        kafkaListener.retryCount(), kafkaListener.retryBackoffTime());
                if (inMemoryRetryConsumer.offer(result, policy)) {
                    // Accepted for retry, so the offset may advance.
                    yield true;
                }
                // Capacity exhausted: withhold the offset rather than silently drop the record.
                LOG.warn("In-memory retry refused for {}-{}@{}; holding the offset back.",
                        record.topic(), record.partition(), record.offset());
                yield false;
            }
            default -> true;
        };
    }
}
