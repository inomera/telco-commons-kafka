package com.inomera.telco.commons.example.springkafka.util;

public interface ThreadUtils {
    static void sleepQuietly(int sleepMs) {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            // ignore
        }
    }
}
