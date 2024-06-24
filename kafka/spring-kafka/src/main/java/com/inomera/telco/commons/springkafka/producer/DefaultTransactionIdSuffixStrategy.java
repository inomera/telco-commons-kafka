package com.inomera.telco.commons.springkafka.producer;

import com.inomera.telco.commons.springkafka.fault.NoProducerAvailableException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@link TransactionIdSuffixStrategy} implementation for managing transactional producer suffixes.
 * If the {@link #maxCache} is greater than 0, the suffixes will be cached and reused.
 * Otherwise, the suffixes will be generated on the fly.
 *
 * <p>
 * The cache is per `transactional.id` prefix. The cache size is limited by the {@link #maxCache}.
 * A value of 0 means no caching and restriction, so the `transactional.id` will be generated on the fly.
 */
public class DefaultTransactionIdSuffixStrategy implements TransactionIdSuffixStrategy {
    private final AtomicInteger transactionIdSuffix = new AtomicInteger();

    private final Map<String, BlockingQueue<String>> suffixCache = new ConcurrentHashMap<>();

    private final int maxCache;

    /**
     * Construct a transaction id suffix strategy with the provided size of the cache.
     *
     * @param maxCache the maximum size of the cache.
     */
    public DefaultTransactionIdSuffixStrategy(int maxCache) {
        Assert.isTrue(maxCache >= 0, "'maxCache' must be greater than or equal to 0");
        this.maxCache = maxCache;
    }

    /**
     * Acquire the suffix for the transactional producer from the cache or generate a new one
     * if caching is disabled.
     *
     * @param txIdPrefix the transaction id prefix.
     * @return the suffix.
     * @throws NoProducerAvailableException if caching is enabled and no suffixes are available.
     */
    @Override
    public synchronized String acquireSuffix(String txIdPrefix) {
        Assert.notNull(txIdPrefix, "'txIdPrefix' must not be null");
        BlockingQueue<String> cache = getSuffixCache(txIdPrefix);
        if (cache == null) {
            return String.valueOf(this.transactionIdSuffix.getAndIncrement());
        }

        String suffix = cache.poll();
        if (suffix == null) {
            throw new NoProducerAvailableException("No available transaction producer", txIdPrefix);
        }
        return suffix;
    }

    @Override
    public void releaseSuffix(String txIdPrefix, String suffix) {
        Assert.notNull(txIdPrefix, "'txIdPrefix' must not be null");
        Assert.notNull(suffix, "'suffix' must not be null");
        if (this.maxCache <= 0) {
            return;
        }
        BlockingQueue<String> queue = getSuffixCache(txIdPrefix);
        if (queue != null && !queue.contains(suffix)) {
            queue.add(suffix);
        }
    }

    @Nullable
    private BlockingQueue<String> getSuffixCache(String txIdPrefix) {
        if (this.maxCache <= 0) {
            return null;
        }

        return this.suffixCache.computeIfAbsent(txIdPrefix, txId -> {
            BlockingQueue<String> queue = new LinkedBlockingQueue<>();
            for (int suffix = 0; suffix < this.maxCache; suffix++) {
                queue.add(String.valueOf(this.transactionIdSuffix.getAndIncrement()));
            }
            return queue;
        });
    }

}
