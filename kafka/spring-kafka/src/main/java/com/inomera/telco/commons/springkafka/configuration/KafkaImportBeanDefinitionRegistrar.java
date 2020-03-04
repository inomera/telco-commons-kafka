package com.inomera.telco.commons.springkafka.configuration;

import com.inomera.telco.commons.springkafka.annotation.KafkaListenerAnnotationBeanPostProcessor;
import com.inomera.telco.commons.springkafka.consumer.invoker.ListenerMethodRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author Serdar Kuzucu
 */
public class KafkaImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
    private static final String POST_PROCESSOR_BEAN_NAME = "bean.KafkaListenerAnnotationBeanPostProcessor";
    private static final String LISTENER_METHOD_REGISTRY_BEAN_NAME = "bean.ListenerMethodRegistry";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (!registry.containsBeanDefinition(POST_PROCESSOR_BEAN_NAME)) {
            registry.registerBeanDefinition(POST_PROCESSOR_BEAN_NAME, new RootBeanDefinition(
                    KafkaListenerAnnotationBeanPostProcessor.class));
        }

        if (!registry.containsBeanDefinition(LISTENER_METHOD_REGISTRY_BEAN_NAME)) {
            registry.registerBeanDefinition(LISTENER_METHOD_REGISTRY_BEAN_NAME, new RootBeanDefinition(
                    ListenerMethodRegistry.class));
        }
    }
}
