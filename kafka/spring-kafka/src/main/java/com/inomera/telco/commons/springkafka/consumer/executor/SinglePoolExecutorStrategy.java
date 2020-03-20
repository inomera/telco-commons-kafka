package com.inomera.telco.commons.springkafka.consumer.executor;

import com.inomera.telco.commons.springkafka.util.ThreadPoolExecutorUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author Serdar Kuzucu
 */
public class SinglePoolExecutorStrategy implements ExecutorStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(SinglePoolExecutorStrategy.class);
    private ThreadPoolExecutor threadPoolExecutor;
    private int coreThreadCount;
    private int maxThreadCount;
    private int keepAliveTime;
    private TimeUnit keepAliveTimeUnit;
    private final Supplier<BlockingQueue<Runnable>> queueSupplier;
    private final ThreadFactory threadFactory;

    public SinglePoolExecutorStrategy(int coreThreadCount, int maxThreadCount, int keepAliveTime,
                                      TimeUnit keepAliveTimeUnit, ThreadFactory threadFactory,
                                      Supplier<BlockingQueue<Runnable>> queueSupplier) {
        this.coreThreadCount = coreThreadCount;
        this.maxThreadCount = maxThreadCount;
        this.keepAliveTime = keepAliveTime;
        this.keepAliveTimeUnit = keepAliveTimeUnit;
        this.threadFactory = threadFactory;
        this.queueSupplier = queueSupplier;
    }

    @Override
    public ThreadPoolExecutor get(ConsumerRecord<String, ?> record) {
        return threadPoolExecutor;
    }

    @Override
    public void start() {
        this.threadPoolExecutor = createExecutor();
    }

    @Override
    public void stop() {
        ThreadPoolExecutorUtils.closeGracefully(threadPoolExecutor, LOG, getClass().getSimpleName());
    }

    public void reconfigure(int coreThreadCount, int maxThreadCount) {
        this.coreThreadCount = coreThreadCount;
        this.maxThreadCount = maxThreadCount;
        threadPoolExecutor.setCorePoolSize(coreThreadCount);
        threadPoolExecutor.setMaximumPoolSize(maxThreadCount);
        threadPoolExecutor.prestartAllCoreThreads();
    }

    public void reconfigure(int coreThreadCount, int maxThreadCount, int keepAliveTime, TimeUnit keepAliveTimeUnit) {
        reconfigure(coreThreadCount, maxThreadCount);
        this.keepAliveTime = keepAliveTime;
        this.keepAliveTimeUnit = keepAliveTimeUnit;
        threadPoolExecutor.setKeepAliveTime(keepAliveTime, keepAliveTimeUnit);
        threadPoolExecutor.prestartAllCoreThreads();
    }

    private ThreadPoolExecutor createExecutor() {
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(coreThreadCount, maxThreadCount, keepAliveTime,
                keepAliveTimeUnit, queueSupplier.get(), threadFactory);
        threadPoolExecutor.prestartAllCoreThreads();
        return threadPoolExecutor;
    }
}
