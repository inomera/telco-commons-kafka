package com.inomera.telco.commons.springkafka.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author Serdar Kuzucu
 */
@Getter
@ToString
@AllArgsConstructor
public class ThreadPoolExecutorSpec {
    private final int coreThreadCount;
    private final int maxThreadCount;
    private final int keepAliveTime;
    private final TimeUnit keepAliveTimeUnit;
    private final ThreadFactory threadFactory;
    private final Supplier<BlockingQueue<Runnable>> blockingQueueSupplier;

    public ThreadPoolExecutor createThreadPool() {
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(coreThreadCount, maxThreadCount, keepAliveTime, keepAliveTimeUnit,
                createQueue(), threadFactory);
        threadPoolExecutor.prestartAllCoreThreads();
        return threadPoolExecutor;
    }

    private BlockingQueue<Runnable> createQueue() {
        return blockingQueueSupplier.get();
    }
}
