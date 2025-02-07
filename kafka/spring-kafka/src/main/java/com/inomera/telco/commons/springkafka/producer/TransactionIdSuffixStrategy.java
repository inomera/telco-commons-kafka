package com.inomera.telco.commons.springkafka.producer;

/**
 * The strategy for managing transactional producer suffixes.
 */
public interface TransactionIdSuffixStrategy {

    /**
     * Acquire the suffix for the transactional producer.
     *
     * @param txIdPrefix the transaction id prefix.
     * @return the suffix.
     */
    String acquireSuffix(String txIdPrefix);

    /**
     * Release the suffix for the transactional producer.
     *
     * @param txIdPrefix the transaction id prefix.
     * @param suffix     the suffix.
     */
    void releaseSuffix(String txIdPrefix, String suffix);
}
