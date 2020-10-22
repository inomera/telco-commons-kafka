package com.inomera.telco.commons.springkafka.consumer.invoker;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.FutureTask;

/**
 * @author Serdar Kuzucu
 */
public class MethodInvoker {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodInvoker.class);

    private final String groupId;
    private final ListenerMethodRegistry listenerMethodRegistry;
    private final List<ListenerInvocationInterceptor> interceptors;

    public MethodInvoker(String groupId, ListenerMethodRegistry listenerMethodRegistry, List<ListenerInvocationInterceptor> interceptors) {
        this.groupId = groupId;
        this.listenerMethodRegistry = listenerMethodRegistry;
        this.interceptors = interceptors;
    }

    public FutureTask<ConsumerRecord<String, ?>> addRecord(final ConsumerRecord<String, ?> record) {
        return new FutureTask<>(() -> {
            try {
                final Object msg = record.value();
                if (msg == null) {
                    LOGGER.info("Null received from topic: {}", record.topic());
                    return;
                }

                listenerMethodRegistry.getListenerMethods(groupId, record.topic(), msg.getClass())
                        .forEach(listenerMethod -> invokeListenerMethod(listenerMethod, msg));
            } catch (Exception e) {
                LOGGER.error("Error processing kafka message [{}].", record.value(), e);
            }
        }, record);
    }

    private void invokeListenerMethod(ListenerMethod listenerMethod, Object message) {
        try {
            invokeBeforeInterceptors(message);
            listenerMethod.invoke(message);
        } catch (Exception e) {
            LOGGER.error("Error processing kafka message [{}]", message, e);
        } finally {
            invokeAfterInterceptors(message);
        }
    }

    private void invokeBeforeInterceptors(Object message) {
        interceptors.forEach(interceptor -> interceptor.beforeInvocation(message));
    }

    private void invokeAfterInterceptors(Object message) {
        interceptors.forEach(interceptor -> interceptor.afterInvocation(message));
    }
}
