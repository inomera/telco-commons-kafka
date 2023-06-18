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

    RETRY retry() default RETRY.NONE;

    /*

     */
    enum RETRY {
        NONE,
        RETRY_FROM_BROKER,
        RETRY_IN_MEMORY_TASK
    }
}
