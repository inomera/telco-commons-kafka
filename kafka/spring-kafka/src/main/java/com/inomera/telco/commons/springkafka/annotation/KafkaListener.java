package com.inomera.telco.commons.springkafka.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface KafkaListener {
    String[] topics() default {};

    String groupId() default "";

    boolean includeSubclasses() default false;

    /*
    true : to remove the message before send commit ack to kafka server if there is any failure case occurred in KafkaListener method, so that the message is waiting to kafka topic for processing. The consumer should be restarted or kafka servers are restarted.
    false : to handle failure case in happened, and send commit ack to kafka server.
     */
    boolean retry() default false;
}
