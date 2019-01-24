package com.inomera.telco.commons.springkafka.producer;

public interface PartitionKeyAware {
    String getPartitionKey();
}
