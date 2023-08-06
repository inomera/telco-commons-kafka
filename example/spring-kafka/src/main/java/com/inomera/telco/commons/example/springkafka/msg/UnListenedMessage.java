package com.inomera.telco.commons.example.springkafka.msg;

/**
 * @author Serdar Kuzucu
 */
public class UnListenedMessage extends AbstractMessage {
    private static final long serialVersionUID = 1L;

    public UnListenedMessage() {
        this("");
    }

    public UnListenedMessage(String txKey) {
        super(txKey);
    }

    @Override
    public Integer getId() {
        return 99;
    }
}
