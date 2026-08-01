package com.inomera.telco.commons.springkafka.consumer.poller;

import com.inomera.telco.commons.lang.Assert;
import com.inomera.telco.commons.springkafka.consumer.KafkaConsumerProperties;
import com.inomera.telco.commons.springkafka.consumer.invoker.BulkInvokerResult;
import com.inomera.telco.commons.springkafka.consumer.retry.BulkRecordRetryer;
import com.inomera.telco.commons.springkafka.consumer.retry.DefaultBulkRecordRetryer;
import com.inomera.telco.commons.springkafka.util.InterruptUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Polls records and dispatches one batch per partition.
 *
 * <p>Note this extends {@link AbstractConsumerPoller} directly rather than
 * {@code DefaultConsumerPoller}. It used to subclass it while shadowing seven of its fields,
 * so a fix applied to one poller silently did not apply to the other.
 *
 * @author Turgay Can
 */
public class BulkConsumerPoller extends AbstractConsumerPoller<BulkInvokerResult> {
    private static final Logger LOG = LoggerFactory.getLogger(BulkConsumerPoller.class);

    private BulkConsumerRecordHandler bulkConsumerRecordHandler;
    private BulkRecordRetryer bulkRecordRetryer = new DefaultBulkRecordRetryer();

    public BulkConsumerPoller(KafkaConsumerProperties kafkaConsumerProperties,
                              Deserializer<?> valueDeserializer,
                              boolean autoPartitionPause) {
        super(kafkaConsumerProperties, valueDeserializer, autoPartitionPause);
    }

    public void setConsumerRecordHandler(BulkConsumerRecordHandler bulkConsumerRecordHandler) {
        Assert.notNull(bulkConsumerRecordHandler, "BulkConsumerRecordHandler is null!");
        this.bulkConsumerRecordHandler = bulkConsumerRecordHandler;
    }

    /**
     * @throws UnsupportedOperationException always; a bulk poller needs a
     *         {@link BulkConsumerRecordHandler}. Previously this overload silently wrote to a
     *         shadowed field, leaving the poller with no handler at all.
     */
    @Override
    public void setConsumerRecordHandler(ConsumerRecordHandler consumerRecordHandler) {
        throw new UnsupportedOperationException(
                "BulkConsumerPoller requires a BulkConsumerRecordHandler, not a ConsumerRecordHandler");
    }

    public void setBulkRecordRetryer(BulkRecordRetryer bulkRecordRetryer) {
        Assert.notNull(bulkRecordRetryer, "BulkRecordRetryer is null!");
        this.bulkRecordRetryer = bulkRecordRetryer;
    }

    @Override
    protected void assertReadyToStart() {
        Assert.notNull(bulkConsumerRecordHandler, "BulkConsumerRecordHandler is null!");
    }

    @Override
    protected boolean dispatch(ConsumerRecords<String, ?> records, Collection<TopicPartition> toPause) {
        final KafkaConsumerProperties properties = getKafkaConsumerProperties();

        if (properties.isAtMostOnceBulk() && !commitAllSyncBeforeProcessing()) {
            return false;
        }

        // Iterate per partition. The previous implementation re-read records.records(tp) once
        // per record, which made grouping quadratic in the size of the poll batch.
        for (TopicPartition partition : records.partitions()) {
            final List<? extends ConsumerRecord<String, ?>> partitionRecords = records.records(partition);
            if (partitionRecords.isEmpty()) {
                continue;
            }

            if (properties.isAtMostOnceSingle()) {
                final long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
                if (!commitSyncBeforeProcessing(partition, lastOffset)) {
                    return false;
                }
            }

            final Set<ConsumerRecord<String, ?>> batch = new LinkedHashSet<>(partitionRecords);
            try {
                track(partition, bulkConsumerRecordHandler.handle(batch));
                if (isAutoPartitionPause()) {
                    toPause.add(partition);
                }
            } catch (Exception e) {
                LOG.error("Error dispatching batch of {} records for {}.", batch.size(), partition, e);
                InterruptUtils.interruptIfInterruptedException(e);
            }
        }
        return true;
    }

    @Override
    protected long highestOffset(BulkInvokerResult result) {
        // The batch is offset-ordered and the old code committed iterator().next(), i.e. the
        // *lowest* offset, so every restart re-delivered the rest of the batch.
        long highest = -1L;
        for (ConsumerRecord<String, ?> record : result.getRecords()) {
            highest = Math.max(highest, record.offset());
        }
        return highest;
    }

    @Override
    protected boolean onResultCompleted(BulkInvokerResult result) {
        try {
            return bulkRecordRetryer.checkAndDecide(result);
        } catch (Exception e) {
            LOG.error("Retry handling failed for a batch of {} records: {}",
                    result.getRecords().size(), e.getMessage(), e);
            InterruptUtils.interruptIfInterruptedException(e);
            return false;
        }
    }
}
