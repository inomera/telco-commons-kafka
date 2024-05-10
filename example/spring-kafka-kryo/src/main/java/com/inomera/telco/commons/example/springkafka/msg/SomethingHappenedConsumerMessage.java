package com.inomera.telco.commons.example.springkafka.msg;

import lombok.ToString;

@ToString(callSuper = true)
public class SomethingHappenedConsumerMessage extends AbstractMessage {
    private static final long serialVersionUID = 1L;

    public SomethingHappenedConsumerMessage() {
        this("");
    }

    public SomethingHappenedConsumerMessage(String txKey) {
        super(txKey);
    }

    @Override
    public Integer getId() {
        return 101;
    }
}
