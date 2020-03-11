package com.inomera.telco.commons.springkafka.annotation;

import com.inomera.telco.commons.springkafka.consumer.invoker.ListenerMethod;
import com.inomera.telco.commons.springkafka.consumer.invoker.ListenerMethodRegistry;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author Serdar Kuzucu
 */
public class KafkaListenerAnnotationBeanPostProcessor implements BeanPostProcessor, Ordered {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaListenerAnnotationBeanPostProcessor.class);

    private ListenerMethodRegistry listenerMethodRegistry;

    public KafkaListenerAnnotationBeanPostProcessor(ListenerMethodRegistry listenerMethodRegistry) {
        this.listenerMethodRegistry = listenerMethodRegistry;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        final Class<?> targetClass = AopUtils.getTargetClass(bean);
        final Map<Method, KafkaListener> annotatedMethods = findListenerAnnotatedMethods(targetClass);

        if (annotatedMethods.isEmpty()) {
            LOG.trace("\"No @KafkaListener annotations found on bean type: {}", bean.getClass());
            return bean;
        }

        for (Map.Entry<Method, KafkaListener> entry : annotatedMethods.entrySet()) {
            final Method method = entry.getKey();
            final KafkaListener annotation = entry.getValue();
            processKafkaListener(method, bean, annotation);
        }

        LOG.debug("{} @KafkaListener methods processed on bean '{}': {}", annotatedMethods.size(), beanName,
                annotatedMethods);
        return bean;
    }

    private Map<Method, KafkaListener> findListenerAnnotatedMethods(Class<?> targetClass) {
        return MethodIntrospector.selectMethods(targetClass, this::findListenerAnnotation);
    }

    private KafkaListener findListenerAnnotation(Method method) {
        return AnnotatedElementUtils.findMergedAnnotation(method, KafkaListener.class);
    }

    private void processKafkaListener(Method method, Object bean, KafkaListener annotation) {
        final Method methodToUse = checkProxy(method, bean);
        processListener(methodToUse, bean, annotation);
    }

    private void processListener(Method method, Object bean, KafkaListener annotation) {
        final Class<?>[] parameterTypes = method.getParameterTypes();

        if (parameterTypes.length != 1) {
            throw new IllegalStateException(String.format("Method with name %s cannot be annotated with " +
                    "@KafkaListener because it has invalid number of parameters. @KafkaListener methods " +
                    "can have only one parameter.", method.getName()));
        }

        if (StringUtils.isBlank(annotation.groupId())) {
            throw new IllegalStateException(String.format("@KafkaListener annotation on method %s " +
                    "must have groupId parameter!", method));
        }

        final ListenerMethod listenerMethod = new ListenerMethod(bean, method);
        final Class<?> messageType = parameterTypes[0];

        if (ArrayUtils.isEmpty(annotation.topics())) {
            if (annotation.includeSubclasses()) {
                listenerMethodRegistry.addParentTypeListenerMethod(annotation.groupId(), messageType, listenerMethod);
            } else {
                listenerMethodRegistry.addListenerMethod(annotation.groupId(), messageType, listenerMethod);
            }
            return;
        }

        for (String topic : annotation.topics()) {
            if (annotation.includeSubclasses()) {
                listenerMethodRegistry.addParentTypeListenerMethod(annotation.groupId(), topic, messageType, listenerMethod);
            } else {
                listenerMethodRegistry.addListenerMethod(annotation.groupId(), topic, messageType, listenerMethod);
            }
        }
    }

    private Method checkProxy(Method methodArg, Object bean) {
        Method method = methodArg;
        if (AopUtils.isJdkDynamicProxy(bean)) {
            try {
                // Found a @KafkaListener method on the target class for this JDK proxy ->
                // is it also present on the proxy itself?
                method = bean.getClass().getMethod(method.getName(), method.getParameterTypes());
                Class<?>[] proxiedInterfaces = ((Advised) bean).getProxiedInterfaces();
                for (Class<?> proxiedInterface : proxiedInterfaces) {
                    try {
                        method = proxiedInterface.getMethod(method.getName(), method.getParameterTypes());
                        break;
                    } catch (@SuppressWarnings("unused") NoSuchMethodException noMethod) {
                        // NOSONAR
                    }
                }
            } catch (SecurityException ex) {
                ReflectionUtils.handleReflectionException(ex);
            } catch (NoSuchMethodException ex) {
                throw new IllegalStateException(String.format(
                        "@KafkaListener method '%s' found on bean target class '%s', " +
                                "but not found in any interface(s) for bean JDK proxy. Either " +
                                "pull the method up to an interface or switch to subclass (CGLIB) " +
                                "proxies by setting proxy-target-class/proxyTargetClass " +
                                "attribute to 'true'", method.getName(),
                        method.getDeclaringClass().getSimpleName()), ex);
            }
        }
        return method;
    }
}
