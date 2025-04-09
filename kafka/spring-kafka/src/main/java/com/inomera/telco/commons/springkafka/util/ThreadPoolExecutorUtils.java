package com.inomera.telco.commons.springkafka.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Serdar Kuzucu
 * @author Turgay Can
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ThreadPoolExecutorUtils {

    public static void closeGracefully(ThreadPoolExecutor ex, Logger log, String exName) {
        if (ex != null) {
            log.info("{} executor is shutting down. please wait..", exName);

            ex.shutdown();

            try {
                boolean finished;
                do {
                    finished = ex.awaitTermination(2, TimeUnit.SECONDS);
                } while (!finished);

                log.info("{} executor is closed gracefully. thanks for waiting..", exName);
            } catch (InterruptedException e) {
                log.info("{} execution queue interrupted while shutting down", exName);
                Thread.currentThread().interrupt();
            }
        }
    }
}
