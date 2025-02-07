package com.inomera.telco.commons.example.springkafka;

import com.inomera.telco.commons.example.springkafka.msg.AbstractMessage;
import com.inomera.telco.commons.example.springkafka.msg.Message;
import com.inomera.telco.commons.example.springkafka.msg.SomethingHappenedConsumerMessage;
import com.inomera.telco.commons.example.springkafka.util.ThreadUtils;
import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.inomera.telco.commons.springkafka.annotation.KafkaListener.RETRY.*;


/**
 * @author Serdar Kuzucu
 */
@Component
public class KryoSomeListener {
    private static final Logger LOG = LoggerFactory.getLogger(KryoSomeListener.class);
    private static final MessageContextHolder messageHolder = new MessageContextHolder();

    @KafkaListener(groupId = "event-logger", topics = {"mouse-event.click", "mouse-event.dblclick"}, includeSubclasses = true, retry = RETRY_IN_MEMORY_TASK, retryCount = 5, retryBackoffTime = 5000L)
    public void handle(Message message) {
        LOG.info("handle : message={}", message);
        ThreadUtils.sleepQuietly(300);
        if (message instanceof SomethingHappenedConsumerMessage) {
            final SomethingHappenedConsumerMessage msg = (SomethingHappenedConsumerMessage) message;
            if (msg.getTime() % 2 == 0) {
                LOG.warn("Commit key={}, msg={}", msg.getTxKey(), msg);
                return;
            }
            throw new RuntimeException("retry test single message consumer without retry");
        }
    }

    @KafkaListener(groupId = "bulk-event-logger", topics = {"mouse-bulk-event.click"}, includeSubclasses = true, retry = NONE)
    public void bulkHandleClick(Set<AbstractMessage> messages) {
        final Message message = messages.iterator().next();
        LOG.info("handle : message={}, messageCount={}", message, messages.size());
        ThreadUtils.sleepQuietly(50);
        if (message instanceof SomethingHappenedConsumerMessage) {
            final SomethingHappenedConsumerMessage msg = (SomethingHappenedConsumerMessage) message;
            if (msg.getTime() % 2 == 0) {
                LOG.warn("Commit key={}, msg={}", msg.getTxKey(), msg);
                return;
            }
            throw new RuntimeException("retry test bulk message consumer without retry");
        }
    }

    @KafkaListener(groupId = "retry-bulk-event-logger", topics = {"mouse-bulk-event.dblclick"}, includeSubclasses = true, retry = RETRY_FROM_BROKER, retryCount = 3)
    public void bulkHandleDoubleClick(Set<AbstractMessage> messages) {
        final Message message = messages.iterator().next();
        LOG.info("handle : message={}, messageCount={}, messageHolderSize={}", message, messages.size(), messageHolder.size());
        for (AbstractMessage msg : messages) {
            if (msg == null) {
                continue;
            }
            final String txKey = msg.getTxKey();
            if (messageHolder.containsKey(txKey)) {
                LOG.warn("Duplicate key={}, msg={}", txKey, msg);
                continue;
            }
            messageHolder.put(txKey, msg);
        }

        ThreadUtils.sleepQuietly(2);
        if (message instanceof SomethingHappenedConsumerMessage) {
            final SomethingHappenedConsumerMessage msg = (SomethingHappenedConsumerMessage) message;
            if (msg.getTime() % 2 == 0) {
                LOG.warn("Commit key={}, msg={}", msg.getTxKey(), msg);
                return;
            }
            throw new RuntimeException("retry test bulk message with message broker");
        }
    }

    @KafkaListener(groupId = "retry-bulk-event-logger", topics = {"mouse-bulk-event.dblclick"}, includeSubclasses = true, retry = RETRY_IN_MEMORY_TASK, retryCount = 3)
    public void bulkHandleInMemoryDoubleClick(Set<AbstractMessage> messages) {
        final Message message = messages.iterator().next();
        LOG.info("handle : message={}, messageCount={}, messageHolderSize={}", message, messages.size(), messageHolder.size());
        for (AbstractMessage msg : messages) {
            if (msg == null) {
                continue;
            }
            final String txKey = msg.getTxKey();
            if (messageHolder.containsKey(txKey)) {
                LOG.warn("Duplicate key={}, msg={}", txKey, msg);
                continue;
            }
            messageHolder.put(txKey, msg);
        }

        ThreadUtils.sleepQuietly(5);
        if (message instanceof SomethingHappenedConsumerMessage) {
            final SomethingHappenedConsumerMessage msg = (SomethingHappenedConsumerMessage) message;
            if (msg.getTime() % 2 == 0) {
                LOG.info("Commit key={}, msg={}", msg.getTxKey(), msg);
                return;
            }
            //throw new RuntimeException("retry test bulk message with in memory retry");
        }
    }

}
