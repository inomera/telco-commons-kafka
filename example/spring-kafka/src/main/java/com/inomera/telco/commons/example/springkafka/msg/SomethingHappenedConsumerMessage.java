package com.inomera.telco.commons.example.springkafka.msg;

import lombok.ToString;

/**
 * @author Serdar Kuzucu
 */
@ToString(callSuper = true)
public class SomethingHappenedConsumerMessage extends AbstractMessage {
    private static final long serialVersionUID = 1L;
}
