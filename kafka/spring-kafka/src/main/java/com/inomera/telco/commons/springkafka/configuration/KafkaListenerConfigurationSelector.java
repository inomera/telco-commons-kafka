package com.inomera.telco.commons.springkafka.configuration;

import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;

@Order
public class KafkaListenerConfigurationSelector implements DeferredImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[]{KafkaImportBeanDefinitionRegistrar.class.getName()};
    }

}
