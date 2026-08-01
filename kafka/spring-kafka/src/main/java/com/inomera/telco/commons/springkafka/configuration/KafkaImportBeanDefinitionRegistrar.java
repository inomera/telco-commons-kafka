package com.inomera.telco.commons.springkafka.configuration;

import com.inomera.telco.commons.springkafka.annotation.KafkaListenerAnnotationBeanPostProcessor;
import com.inomera.telco.commons.springkafka.consumer.invoker.ListenerMethodRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;

/**
 * @author Serdar Kuzucu
 */
public class KafkaImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
    private static final String POST_PROCESSOR_BEAN_NAME = "bean.KafkaListenerAnnotationBeanPostProcessor";
    private static final String LISTENER_METHOD_REGISTRY_BEAN_NAME = "bean.ListenerMethodRegistry";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (!registry.containsBeanDefinition(POST_PROCESSOR_BEAN_NAME)) {
            RootBeanDefinition beanDefinition = new RootBeanDefinition(
                    KafkaListenerAnnotationBeanPostProcessor.class);
            beanDefinition.setRole(ROLE_INFRASTRUCTURE);
            registry.registerBeanDefinition(POST_PROCESSOR_BEAN_NAME, beanDefinition);
        }

        if (!registry.containsBeanDefinition(LISTENER_METHOD_REGISTRY_BEAN_NAME)) {
            RootBeanDefinition beanDefinition = new RootBeanDefinition(
                    ListenerMethodRegistry.class);
            beanDefinition.setRole(ROLE_INFRASTRUCTURE);
            registry.registerBeanDefinition( LISTENER_METHOD_REGISTRY_BEAN_NAME, beanDefinition);
        }
    }
}
