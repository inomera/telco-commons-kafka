package com.inomera.telco.commons.example.springkafka.msg;

import lombok.ToString;

/**
 * @author Serdar Kuzucu
 */
@ToString(callSuper = true)
public class SomethingHappenedBeautifullyMessage extends SomethingHappenedMessage implements BeautifulMessage {
    private static final long serialVersionUID = 1L;
}
