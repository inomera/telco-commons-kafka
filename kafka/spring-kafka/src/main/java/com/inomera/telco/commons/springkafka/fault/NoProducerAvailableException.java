package com.inomera.telco.commons.springkafka.fault;

import lombok.Getter;

import java.io.Serial;

/**
 * Exception when no producer is available.
 */
@Getter
public class NoProducerAvailableException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * -- GETTER --
     *  Return the transaction id prefix that was used to create the producer and failed.
     *
     * @return the transaction id prefix.
     */
    private final String txIdPrefix;

    /**
     * Constructs a new no producer available exception with the specified detail message.
     *
     * @param message    the message.
     * @param txIdPrefix the transaction id prefix.
     */
    public NoProducerAvailableException(String message, String txIdPrefix) {
        super(message);
        this.txIdPrefix = txIdPrefix;
    }

}
