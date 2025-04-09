package com.inomera.telco.commons.springkafka.consumer;

/**
 * @author Serdar Kuzucu
 * @author Turgay Can
 */
public interface PollerThreadNotifier {

    void alarm(String alarmText);

    void alarm(String alarmText, Exception e);
}
