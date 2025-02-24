package com.inomera.telco.commons.springkafka.builder.virtual;

import com.inomera.telco.commons.lang.thread.IncrementalNamingUnstartedVirtualThreadFactory;
import com.inomera.telco.commons.springkafka.consumer.executor.virtual.VirtualExecutorStrategy;

import java.util.concurrent.ThreadFactory;

import static com.inomera.telco.commons.springkafka.SpringKafkaConstants.INVOKER_THREAD_NAME_FORMAT;

/**
 * @author Turgay Can
 */
public interface VirtualExecutorStrategyBuilder {

    default ThreadFactory getThreadFactory(ThreadFactory threadFactory, String infix) {
        if (threadFactory != null) {
            return threadFactory;
        }

        return new IncrementalNamingUnstartedVirtualThreadFactory(String.format(INVOKER_THREAD_NAME_FORMAT, infix));
    }

    VirtualExecutorStrategy build(String groupId);
}
