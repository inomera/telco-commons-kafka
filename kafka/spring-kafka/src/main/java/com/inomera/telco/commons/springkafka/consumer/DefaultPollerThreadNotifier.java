package com.inomera.telco.commons.springkafka.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultPollerThreadNotifier implements PollerThreadNotifier {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultPollerThreadNotifier.class);

    @Override
    public void alarm(String alarmText) {
        LOG.warn("Consumer is not re-started!! {}", alarmText);
    }

    @Override
    public void alarm(String alarmText, Exception e) {
        LOG.warn("Consumer is not re-started!! {}, Exception :: {}", alarmText, e);
    }
}
