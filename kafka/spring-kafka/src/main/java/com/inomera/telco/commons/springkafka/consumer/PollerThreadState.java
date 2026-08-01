package com.inomera.telco.commons.springkafka.consumer;

import com.inomera.telco.commons.springkafka.consumer.poller.ConsumerPoller;
import lombok.Data;

@Data
public class PollerThreadState {
    private Long threadId;
    private String hostname;
    private String threadName;
    private String oldJvmState;
    private String currentJvmState;
    private ConsumerPoller consumerPoller;

    /**
     * The poller thread itself. Liveness used to be inferred by scanning
     * {@code Thread.getAllStackTraces()}, which is expensive and does not report virtual
     * threads at all; holding the reference makes {@code isAlive()} an O(1) check.
     */
    private transient Thread thread;

    @Override
    public String toString() {
        return "PollerThreadState{" +
                "threadId=" + threadId +
                ", hostname=" + hostname +
                ", threadName='" + threadName + '\'' +
                ", oldJvmState='" + oldJvmState + '\'' +
                ", currentJvmState='" + currentJvmState + '\'' +
                "}\n";
    }
}
