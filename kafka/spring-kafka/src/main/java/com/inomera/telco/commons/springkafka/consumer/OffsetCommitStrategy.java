package com.inomera.telco.commons.springkafka.consumer;

/**
 * @author Ramazan Karakaya
 */
public enum OffsetCommitStrategy {
    AT_LEAST_ONCE_BULK,
    AT_LEAST_ONCE_SINGLE,
    AT_MOST_ONCE_BULK,
    AT_MOST_ONCE_SINGLE
}
