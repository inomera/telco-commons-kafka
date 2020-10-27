package com.inomera.telco.commons.springkafka.util;

/**
 * @author Serdar Kuzucu
 */
public class InterruptUtils {
    public static void interruptIfInterruptedException(Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
