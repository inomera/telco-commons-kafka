package com.inomera.telco.commons.springkafka.consumer.invoker;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Set;
import java.util.concurrent.FutureTask;

/**
 * @author Serdar Kuzucu
 */
public interface RejectionHandler {
    void handleReject(final ConsumerRecord<String, ?> record, final FutureTask<InvokerResult> futureTask);

    void handleReject(final Set<ConsumerRecord<String, ?>> records, final FutureTask<BulkInvokerResult> futureTask);

    void start();

    void stop();
}
