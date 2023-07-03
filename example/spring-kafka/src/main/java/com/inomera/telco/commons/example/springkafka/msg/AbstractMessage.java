package com.inomera.telco.commons.example.springkafka.msg;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Serdar Kuzucu
 */
@Getter
@Setter
@AllArgsConstructor
@ToString
public abstract class AbstractMessage implements Message {
    private static final long serialVersionUID = 1L;

    private final long time = System.currentTimeMillis();

    @Override
    public String getPartitionKey() {
        return String.valueOf(time);
    }

    private String txKey;

}
