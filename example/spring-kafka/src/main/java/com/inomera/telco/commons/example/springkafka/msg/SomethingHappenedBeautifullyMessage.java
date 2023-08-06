package com.inomera.telco.commons.example.springkafka.msg;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Serdar Kuzucu
 */
@Getter
@Setter
@ToString(callSuper = true)
public class SomethingHappenedBeautifullyMessage extends SomethingHappenedMessage implements BeautifulMessage {
    private static final long serialVersionUID = 1L;

    public SomethingHappenedBeautifullyMessage() {
    }

    public SomethingHappenedBeautifullyMessage(String txKey) {
        super(txKey);
    }

    @Override
    public Integer getId() {
        return 100;
    }
}

