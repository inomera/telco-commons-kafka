package com.inomera.telco.commons.springkafka.consumer.retry;

import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import com.inomera.telco.commons.springkafka.consumer.invoker.BulkInvokerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bulk counterpart of {@link DefaultRecordRetryer}. See that class for why
 * {@code RETRY_FROM_BROKER} no longer throws out of the poll loop.
 *
 * @author Turgay Can
 */
public class DefaultBulkRecordRetryer implements BulkRecordRetryer {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultBulkRecordRetryer.class);

    private final InMemoryBulkRecordRetryConsumer inMemoryBulkRetryConsumer;
    /** See {@link DefaultRecordRetryer}. */
    private final boolean deferCommitUntilRetryResolves;

    public DefaultBulkRecordRetryer() {
        this(null);
    }

    public DefaultBulkRecordRetryer(InMemoryBulkRecordRetryConsumer inMemoryBulkRetryConsumer) {
        this.inMemoryBulkRetryConsumer = inMemoryBulkRetryConsumer;
        this.deferCommitUntilRetryResolves =
                inMemoryBulkRetryConsumer != null && inMemoryBulkRetryConsumer.reportsRetryOutcome();
    }

    @Override
    public void checkAndRetry(BulkInvokerResult result) {
        checkAndDecide(result);
    }

    @Override
    public boolean checkAndDecide(BulkInvokerResult result) {
        final KafkaListener kafkaListener = result.getKafkaListener();
        if (kafkaListener == null || kafkaListener.retry() == KafkaListener.RETRY.NONE) {
            return true;
        }

        final int batchSize = result.getRecords().size();

        return switch (kafkaListener.retry()) {
            case RETRY_FROM_BROKER -> {
                LOG.warn("Holding offsets back for a batch of {} records; the broker will re-deliver them.",
                        batchSize);
                yield false;
            }
            case RETRY_IN_MEMORY_TASK -> {
                if (inMemoryBulkRetryConsumer == null) {
                    LOG.warn("RETRY_IN_MEMORY_TASK requested for a batch of {} records but no retry consumer "
                            + "is wired; holding the offsets back instead.", batchSize);
                    yield false;
                }
                final RetryPolicy policy = RetryPolicy.fromListener(
                        kafkaListener.retryCount(), kafkaListener.retryBackoffTime());
                if (inMemoryBulkRetryConsumer.offer(result, policy)) {
                    // See DefaultRecordRetryer: hold the offsets until the retry resolves so a
                    // crash mid-retry cannot lose the batch.
                    yield !deferCommitUntilRetryResolves;
                }
                LOG.warn("In-memory retry refused for a batch of {} records; holding the offsets back.", batchSize);
                yield false;
            }
            default -> true;
        };
    }
}
