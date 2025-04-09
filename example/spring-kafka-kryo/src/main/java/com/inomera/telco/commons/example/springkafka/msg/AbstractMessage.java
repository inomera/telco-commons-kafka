package com.inomera.telco.commons.example.springkafka.msg;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;

/**
 * @author Serdar Kuzucu
 */
@Getter
@Setter
@AllArgsConstructor
@ToString
public abstract class AbstractMessage implements Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final long time = System.currentTimeMillis();

    @Override
    public String getPartitionKey() {
        return String.valueOf(time);
    }

    private String txKey;

}
