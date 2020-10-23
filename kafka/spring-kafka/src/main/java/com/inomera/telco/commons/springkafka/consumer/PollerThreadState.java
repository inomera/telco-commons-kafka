package com.inomera.telco.commons.springkafka.consumer;

import lombok.Data;

@Data
public class PollerThreadState {
    private Long threadId;
    private String hostname;
    private String threadName;
    private String oldJvmState;
    private String currentJvmState;
    private KafkaMessageConsumer kafkaMessageConsumer;

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
