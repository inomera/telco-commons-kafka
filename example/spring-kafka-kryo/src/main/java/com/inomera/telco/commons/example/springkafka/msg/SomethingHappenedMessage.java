package com.inomera.telco.commons.example.springkafka.msg;

import lombok.ToString;

/**
 * @author Serdar Kuzucu
 */
@ToString(callSuper = true)
public class SomethingHappenedMessage extends AbstractMessage {
    private static final long serialVersionUID = 1L;

    public SomethingHappenedMessage() {
        this("");
    }

    public SomethingHappenedMessage(String txKey) {
        super(txKey);
    }

    @Override
    public Integer getId() {
        return 102;
    }
}
