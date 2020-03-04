package com.inomera.telco.commons.example.springkafka.msg;

import lombok.ToString;

/**
 * @author Serdar Kuzucu
 */
@ToString
public abstract class AbstractMessage implements Message {
    private static final long serialVersionUID = 1L;

    private final long time = System.currentTimeMillis();

    @Override
    public String getPartitionKey() {
        return String.valueOf(time);
    }
}
