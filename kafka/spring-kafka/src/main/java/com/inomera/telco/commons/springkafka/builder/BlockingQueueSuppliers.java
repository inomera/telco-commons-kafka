package com.inomera.telco.commons.springkafka.builder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Supplier;

/**
 * @author Serdar Kuzucu
 */
public class BlockingQueueSuppliers {
    static Supplier<BlockingQueue<Runnable>> forCapacity(int capacity) {
        if (capacity == 0) {
            return SynchronousQueue::new;
        }

        return () -> new LinkedBlockingQueue<>(capacity);
    }
}
