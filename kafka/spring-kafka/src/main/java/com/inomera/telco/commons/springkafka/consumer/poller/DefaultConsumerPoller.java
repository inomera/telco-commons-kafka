package com.inomera.telco.commons.springkafka.consumer.poller;

import com.inomera.telco.commons.lang.Assert;
import com.inomera.telco.commons.springkafka.consumer.KafkaConsumerProperties;
import com.inomera.telco.commons.springkafka.consumer.invoker.InvokerResult;
import com.inomera.telco.commons.springkafka.consumer.retry.DefaultRecordRetryer;
import com.inomera.telco.commons.springkafka.consumer.retry.RecordRetryer;
import com.inomera.telco.commons.springkafka.util.InterruptUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Polls records and dispatches them one at a time.
 *
 * @author Serdar Kuzucu
 * @author Turgay Can
 */
public class DefaultConsumerPoller extends AbstractConsumerPoller<InvokerResult> {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultConsumerPoller.class);

    private ConsumerRecordHandler consumerRecordHandler;
    private RecordRetryer recordRetryer = new DefaultRecordRetryer();

    public DefaultConsumerPoller(KafkaConsumerProperties kafkaConsumerProperties,
                                 Deserializer<?> valueDeserializer,
                                 boolean autoPartitionPause) {
        super(kafkaConsumerProperties, valueDeserializer, autoPartitionPause);
    }

    @Override
    public void setConsumerRecordHandler(ConsumerRecordHandler consumerRecordHandler) {
        Assert.notNull(consumerRecordHandler, "ConsumerRecordHandler is null!");
        this.consumerRecordHandler = consumerRecordHandler;
    }

    public void setRecordRetryer(RecordRetryer recordRetryer) {
        Assert.notNull(recordRetryer, "RecordRetryer is null!");
        this.recordRetryer = recordRetryer;
    }

    @Override
    protected void assertReadyToStart() {
        Assert.notNull(consumerRecordHandler, "ConsumerRecordHandler is null!");
    }

    @Override
    protected boolean dispatch(ConsumerRecords<String, ?> records, Collection<TopicPartition> toPause) {
        final KafkaConsumerProperties properties = getKafkaConsumerProperties();

        if (properties.isAtMostOnceBulk() && !commitAllSyncBeforeProcessing()) {
            return false;
        }

        for (ConsumerRecord<String, ?> rec : records) {
            final TopicPartition partition = new TopicPartition(rec.topic(), rec.partition());

            if (properties.isAtMostOnceSingle() && !commitSyncBeforeProcessing(partition, rec.offset())) {
                return false;
            }

            try {
                track(partition, consumerRecordHandler.handle(rec));
                if (isAutoPartitionPause()) {
                    toPause.add(partition);
                }
            } catch (Exception e) {
                LOG.error("Error dispatching record {}-{}@{}.", rec.topic(), rec.partition(), rec.offset(), e);
                InterruptUtils.interruptIfInterruptedException(e);
            }
        }
        return true;
    }

    @Override
    protected long highestOffset(InvokerResult result) {
        return result.getRecord().offset();
    }

    @Override
    protected boolean onResultCompleted(InvokerResult result) {
        try {
            return recordRetryer.checkAndDecide(result);
        } catch (Exception e) {
            // Never let a retry decision escape into the poll loop: that used to kill the poller.
            LOG.error("Retry handling failed for {}-{}@{}: {}",
                    result.getRecord().topic(), result.getRecord().partition(),
                    result.getRecord().offset(), e.getMessage(), e);
            InterruptUtils.interruptIfInterruptedException(e);
            // Withhold the offset so the record is not silently lost.
            return false;
        }
    }
}
