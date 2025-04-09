package com.inomera.telco.commons.springkafka;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Serdar Kuzucu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SpringKafkaConstants {
    public static final String CONSUMER_POLLER_THREAD_NAME_FORMAT = "consumer-poller-%s-";
    public static final String INVOKER_THREAD_NAME_FORMAT = "kafka-method-invoker-%s-";
    public static final String RETRY_THREAD_NAME_FORMAT = "kafka-memory-retryer";
}
