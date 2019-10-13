package com.inomera.telco.commons.example.springkafka.msg;

import lombok.ToString;

/**
 * @author Serdar Kuzucu
 */
@ToString
public abstract class AbstractMessage implements Message {
    private static final long serialVersionUID = 1L;
}
