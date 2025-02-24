package com.inomera.telco.commons.springkafka.consumer.executor;

import com.inomera.telco.commons.springkafka.consumer.executor.virtual.PartitionKeyAwareVirtualExecutorStrategy;
import com.inomera.telco.commons.springkafka.util.ThreadPoolExecutorSpec;
import com.inomera.telco.commons.springkafka.util.ThreadPoolExecutorUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;

/**
 * DynamicNamedExecutorStrategy is an implementation of the {@link ExecutorStrategy} interface.
 * It provides the ability to dynamically manage named thread pool executors, allowing
 * fine-grained control over the allocation of tasks to specific executors based on their
 * associated names. This strategy is particularly useful for managing thread pools in scenarios
 * where tasks need to be processed in thread pools that are dynamically determined.
 *
 * <p>There are two main use cases for this strategy:
 * <ul>
 *     <li><b>IO-bound tasks:</b> Use {@link PartitionKeyAwareVirtualExecutorStrategy} for tasks
 *         involving IO operations such as database queries, API calls, or messaging. This leverages virtual threads.</li>
 *     <li><b>CPU-bound tasks:</b> Use this class for processor-intensive operations,
 *         such as multi-threaded computations, leveraging OS threads.</li>
 * </ul>
 *
 * Features:
 * <ul>
 *     <li>Dynamic creation and configuration of thread pool executors with named keys.</li>
 *     <li>Ability to map {@link ConsumerRecord} objects to specific executors using custom logic.</li>
 *     <li>Pre-configuration and management of default executor instances.</li>
 *     <li>Thread safety ensured via synchronization mechanisms.</li>
 * </ul>
 *
 * Example Usage:
 * <pre>{@code
 * DynamicNamedExecutorStrategy strategy = new DynamicNamedExecutorStrategy(
 *     defaultExecutorSpec, consumerRecord -> "customExecutorName");
 *
 * strategy.configureExecutor("executorName", customExecutorSpec);
 * ConsumerRecord record = ...;
 * ThreadPoolExecutor executor = strategy.get(record);
 * }</pre>
 *
 * @author Serdar Kuzucu
 * @author Turgay Can
 */
public class DynamicNamedExecutorStrategy implements ExecutorStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(DynamicNamedExecutorStrategy.class);
    private static final String DEFAULT_EXECUTOR_NAME = "DefaultExecutor";

    private final Map<String, ThreadPoolExecutor> executorMap = new ConcurrentHashMap<>();
    private final Map<String, ThreadPoolExecutorSpec> executorSpecs = new ConcurrentHashMap<>();
    private final Function<ConsumerRecord<String, ?>, String> recordExecutorNameMapper;
    private final Object lock = new Object();
    private ThreadPoolExecutor defaultExecutor;
    private ThreadPoolExecutorSpec defaultExecutorSpec;

    public DynamicNamedExecutorStrategy(ThreadPoolExecutorSpec defaultExecutorSpec,
                                        Function<ConsumerRecord<String, ?>, String> recordExecutorNameMapper) {
        this.recordExecutorNameMapper = recordExecutorNameMapper;
        this.defaultExecutorSpec = defaultExecutorSpec;
    }

    @Override
    public ThreadPoolExecutor get(ConsumerRecord<String, ?> record) {
        final String executorName = recordExecutorNameMapper.apply(record);
        return getByExecutorName(executorName);
    }

    private ThreadPoolExecutor getByExecutorName(String executorName) {
        if (!executorSpecs.containsKey(executorName)) {
            return defaultExecutor;
        }
        createExecutorIfNotExist(executorName);
        return executorMap.get(executorName);
    }

    public void configureDefaultExecutor(ThreadPoolExecutorSpec executorSpec) {
        synchronized (lock) {
            final ThreadPoolExecutorSpec existingConfig = this.defaultExecutorSpec;
            this.defaultExecutorSpec = executorSpec;
            if (this.defaultExecutor == null) {
                // Strategy has not been started yet.
                return;
            }

            final int startedNewThreads = configureExecutorAndPreStartThreads(this.defaultExecutor, executorSpec);
            LOG.info("configureDefaultExecutor::pool [{}] configured. oldConfig={}, newConfig={}, startedNewThreads={}",
                    DEFAULT_EXECUTOR_NAME, existingConfig, executorSpec, startedNewThreads);
        }
    }

    public void configureExecutor(String executorName, ThreadPoolExecutorSpec executorSpec) {
        // We will replace if existing thread pool exists
        final ThreadPoolExecutorSpec existingConfig = executorSpecs.put(executorName, executorSpec);
        final ThreadPoolExecutor threadPoolExecutor = getByExecutorName(executorName);
        // Calling `get` on super class will cause a new `ThreadPoolExecutor` creation if not exists.
        // Since we have put the `config` into `executorSpecs`,
        // new ThreadPoolExecutor will be configured with the new config.
        // However, we need to reconfigure it in case it is an existing pool.
        final int startedNewThreads = configureExecutorAndPreStartThreads(threadPoolExecutor, executorSpec);
        LOG.info("configureExecutor::pool [{}] configured. oldConfig={}, newConfig={}, startedNewThreads={}",
                executorName, existingConfig, executorSpec, startedNewThreads);
    }

    private int configureExecutorAndPreStartThreads(ThreadPoolExecutor threadPoolExecutor, ThreadPoolExecutorSpec executorSpec) {
        threadPoolExecutor.setCorePoolSize(executorSpec.getCoreThreadCount());
        threadPoolExecutor.setMaximumPoolSize(executorSpec.getMaxThreadCount());
        threadPoolExecutor.setKeepAliveTime(executorSpec.getKeepAliveTime(), executorSpec.getKeepAliveTimeUnit());
        return threadPoolExecutor.prestartAllCoreThreads();
    }

    public void removeExecutor(String executorName) {
        final ThreadPoolExecutorSpec existingConfig = executorSpecs.remove(executorName);
        // After removing from `executorSpecs`, `get` starts to return "defaultExecutor" for `executorName`
        final ThreadPoolExecutor threadPoolExecutor = removeAndStop(executorName);
        // After removing `ThreadPoolExecutor` from super class,
        // new requests will use `defaultExecutor`.
        LOG.info("removeExecutor::pool [{}] closed for name {}. Existing (now removed) config={}",
                threadPoolExecutor, executorName, existingConfig);
    }

    private ThreadPoolExecutor removeAndStop(String executorName) {
        final ThreadPoolExecutor removedExecutor;
        synchronized (lock) {
            removedExecutor = executorMap.remove(executorName);
            ThreadPoolExecutorUtils.closeGracefully(removedExecutor, LOG, executorName);
        }
        return removedExecutor;
    }

    @Override
    public void start() {
        synchronized (lock) {
            defaultExecutor = defaultExecutorSpec.createThreadPool();
            defaultExecutor.prestartAllCoreThreads();
        }
    }

    @Override
    public void stop() {
        synchronized (lock) {
            executorMap.values().forEach(ThreadPoolExecutor::shutdown);
            defaultExecutor.shutdown();

            ThreadPoolExecutorUtils.closeGracefully(defaultExecutor, LOG, DEFAULT_EXECUTOR_NAME);
            executorMap.forEach((executorName, executor) -> ThreadPoolExecutorUtils.closeGracefully(executor, LOG, executorName));
            executorMap.clear();
        }
    }

    private void createExecutorIfNotExist(String executorName) {
        if (!executorMap.containsKey(executorName)) {
            // block if no executor exists for specified executorName
            synchronized (lock) {
                // re-check to avoid waiting threads to re-create executor again
                if (!executorMap.containsKey(executorName)) {
                    executorMap.put(executorName, createExecutor(executorName));
                }
            }
        }
    }

    private ThreadPoolExecutor createExecutor(String executorName) {
        final ThreadPoolExecutorSpec specForExecutorName = executorSpecs.get(executorName);
        if (specForExecutorName != null) {
            final ThreadPoolExecutor threadPoolExecutor = specForExecutorName.createThreadPool();
            threadPoolExecutor.prestartAllCoreThreads();
            return threadPoolExecutor;
        }
        return defaultExecutor;
    }
}
