package com.inomera.telco.commons.springkafka;

public interface PartitionKeyAware {
    String getPartitionKey();
}
