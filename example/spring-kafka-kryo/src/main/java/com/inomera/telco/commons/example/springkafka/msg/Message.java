package com.inomera.telco.commons.example.springkafka.msg;

import com.inomera.telco.commons.springkafka.PartitionKeyAware;

import java.io.Serializable;

/**
 * @author Serdar Kuzucu
 */
public interface Message extends Serializable, PartitionKeyAware {
}
