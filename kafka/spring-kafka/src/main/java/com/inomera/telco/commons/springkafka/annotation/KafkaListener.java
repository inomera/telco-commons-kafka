package com.inomera.telco.commons.springkafka.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface KafkaListener {
    /*
    Topic names
    */
    String[] topics() default {};

    /*
    Consumer group id value
    */
    String groupId() default "";

    /*
    subscribe the child messages of the parent message
     */
    boolean includeSubclasses() default false;

    /*
    The feature is experimental level
    minimum retry count value. It works for RETRY strategies(excludes NONE)
     */
    int retryCount() default 3;

    /*
    The feature is experimental level
    max retry backoff time value in MILLIS. It works only for RETRY_IN_MEMORY_TASK RETRY strategy
    */
    long retryBackoffTime() default 3000L;

    /*
    The feature is experimental level
    Retry policy works only for below message commit (ack) strategies.
    Default value is NONE.
    com.inomera.telco.commons.springkafka.consumer.OffsetCommitStrategy
    AT_LEAST_ONCE_ONCE
    AT_LEAST_ONCE_BULK
    */
    RETRY retry() default RETRY.NONE;

    /*
    NONE : no retry.
    RETRY_FROM_BROKER : do not ack/commit message to broker! re-start consumer, consumer polls message from broker again. It can cause duplicated dirty messages.
    RETRY_IN_MEMORY_TASK : commit/ack message to broker, retry in consumer local queue. default retryCount val is 3.
     */
    enum RETRY {
	NONE,
	RETRY_FROM_BROKER,
	RETRY_IN_MEMORY_TASK
    }
}
