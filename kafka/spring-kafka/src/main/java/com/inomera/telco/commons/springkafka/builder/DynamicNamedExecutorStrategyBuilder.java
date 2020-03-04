package com.inomera.telco.commons.springkafka.builder;

import com.inomera.telco.commons.lang.Assert;
import com.inomera.telco.commons.lang.thread.IncrementalNamingThreadFactory;
import com.inomera.telco.commons.springkafka.consumer.executor.DynamicNamedExecutorStrategy;
import com.inomera.telco.commons.springkafka.consumer.executor.ExecutorStrategy;
import com.inomera.telco.commons.springkafka.util.ThreadPoolExecutorSpec;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.inomera.telco.commons.springkafka.SpringKafkaConstants.INVOKER_THREAD_NAME_FORMAT;
import static com.inomera.telco.commons.springkafka.builder.BlockingQueueSuppliers.forCapacity;

/**
 * @author Serdar Kuzucu
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class DynamicNamedExecutorStrategyBuilder extends AbstractExecutorStrategyBuilder {
    private final UnorderedProcessingStrategyBuilder unorderedProcessingStrategyBuilder;
    private int defaultCoreThreadCount = 0;
    private int defaultMaxThreadCount = 3;
    private int defaultKeepAliveTime = 1;
    private TimeUnit defaultKeepAliveTimeUnit = TimeUnit.MINUTES;
    private ThreadFactory threadFactory;
    private int queueCapacity = Integer.MAX_VALUE;
    private Function<ConsumerRecord<String, ?>, String> executorNamingFunction;
    private Map<String, ThreadPoolExecutorCapacity> executorConfigurations = new HashMap<>();

    public DynamicNamedExecutorStrategyBuilder configureDefaultExecutor(int coreThreadCount, int maxThreadCount,
                                                                        int keepAliveTime, TimeUnit keepAliveTimeUnit) {
        Assert.isTrue(coreThreadCount <= maxThreadCount, "coreThreadCount should be less than or equal to maxThreadCount");
        this.defaultCoreThreadCount = coreThreadCount;
        this.defaultMaxThreadCount = maxThreadCount;
        this.defaultKeepAliveTime = keepAliveTime;
        this.defaultKeepAliveTimeUnit = keepAliveTimeUnit;
        return this;
    }

    public DynamicNamedExecutorStrategyBuilder configureExecutor(String executorName, int coreThreadCount,
                                                                 int maxThreadCount, int keepAliveTime,
                                                                 TimeUnit keepAliveTimeUnit) {
        Assert.isTrue(coreThreadCount <= maxThreadCount, "coreThreadCount should be less than or equal to maxThreadCount");
        executorConfigurations.put(executorName, new ThreadPoolExecutorCapacity(coreThreadCount, maxThreadCount,
                keepAliveTime, keepAliveTimeUnit));
        return this;
    }

    public DynamicNamedExecutorStrategyBuilder executorNamingFunction(Function<ConsumerRecord<String, ?>, String> executorNamingFunction) {
        this.executorNamingFunction = executorNamingFunction;
        return this;
    }

    public DynamicNamedExecutorStrategyBuilder queueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
        return this;
    }

    public DynamicNamedExecutorStrategyBuilder threadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        return this;
    }

    public UnorderedProcessingStrategyBuilder and() {
        return this.unorderedProcessingStrategyBuilder;
    }

    private Function<ConsumerRecord<String, ?>, String> getExecutorNamingFunction() {
        if (executorNamingFunction == null) {
            return ConsumerRecord::topic;
        }
        return executorNamingFunction;
    }

    private ThreadFactory getThreadFactory(String groupId) {
        if (this.threadFactory != null) {
            return this.threadFactory;
        }
        return new IncrementalNamingThreadFactory(String.format(INVOKER_THREAD_NAME_FORMAT, groupId));
    }

    @Override
    ExecutorStrategy build(String groupId) {
        final int defaultMaxThreadCount = Math.max(this.defaultMaxThreadCount, defaultCoreThreadCount);
        final ThreadFactory threadFactory = getThreadFactory(groupId);
        final ThreadPoolExecutorSpec defaultExecutorSpec = new ThreadPoolExecutorSpec(defaultCoreThreadCount, defaultMaxThreadCount,
                defaultKeepAliveTime, defaultKeepAliveTimeUnit, threadFactory, forCapacity(queueCapacity));
        final DynamicNamedExecutorStrategy strategy = new DynamicNamedExecutorStrategy(defaultExecutorSpec, getExecutorNamingFunction());
        executorConfigurations.forEach((executorName, capacity) -> strategy.configureExecutor(executorName,
                new ThreadPoolExecutorSpec(capacity.coreThreadCount, capacity.maxThreadCount, capacity.keepAliveTime,
                        capacity.keepAliveTimeUnit, threadFactory, forCapacity(queueCapacity))));
        return strategy;
    }

    @AllArgsConstructor
    private static class ThreadPoolExecutorCapacity {
        private final int coreThreadCount;
        private final int maxThreadCount;
        private final int keepAliveTime;
        private final TimeUnit keepAliveTimeUnit;
    }
}
