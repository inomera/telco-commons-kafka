package com.inomera.telco.commons.springkafka.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Serdar Kuzucu
 * @author Turgay Can
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InterruptUtils {

    public static void interruptIfInterruptedException(Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
