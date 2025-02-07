package com.inomera.telco.commons.springkafka.consumer;

import java.io.Closeable;

public interface ThreadStateChecker extends Closeable {
    void check();
}
