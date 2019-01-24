package com.inomera.telco.commons.springkafka.annotation;

import com.inomera.telco.commons.springkafka.configuration.KafkaBootstrapConfiguration;
import com.inomera.telco.commons.springkafka.configuration.KafkaListenerConfigurationSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Serdar Kuzucu
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({KafkaListenerConfigurationSelector.class, KafkaBootstrapConfiguration.class})
public @interface EnableKafkaListeners {
}
