package com.inomera.telco.commons.example.springkafka.util;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public interface ThreadUtils {
    Logger LOG = LoggerFactory.getLogger(ThreadUtils.class);

    static void sleepQuietly(int sleepMs) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            LOG.error("InterruptedException occurred", e);
            // ignore
        }
        stopwatch.stop();
        LOG.info("Duration millis: {}", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
}
