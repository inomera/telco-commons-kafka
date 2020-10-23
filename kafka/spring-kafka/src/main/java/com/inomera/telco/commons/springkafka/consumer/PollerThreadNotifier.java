package com.inomera.telco.commons.springkafka.consumer;

public interface PollerThreadNotifier {

    void alarm(String alarmText);
    void alarm(String alarmText, Exception e);
}
