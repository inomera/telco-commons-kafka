package com.inomera.telco.commons.springkafka.consumer;

/**
 * @author Ramazan Karakaya
 */
public enum OffsetCommitStrategy {
    /*
    At least once bulk meaning is to send ack/commit to kafka broker after handling/processing messages as bulk according to topic partition.
     */
    AT_LEAST_ONCE_BULK,
    /*
    At least once single meaning is to send ack/commit to kafka broker after handling/processing messages as single according to topic partition.
     */
    AT_LEAST_ONCE_SINGLE,
    /*
    At most once bulk meaning is to send ack/commit to kafka broker before handling/processing messages as bulk according to topic partition.
     */
    AT_MOST_ONCE_BULK,
    /*
    At most once single meaning is to send ack/commit to kafka broker before handling/processing messages as single according to topic partition.
     */
    AT_MOST_ONCE_SINGLE
}
