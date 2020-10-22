package com.inomera.telco.commons.springkafka.consumer.invoker;

import com.inomera.telco.commons.springkafka.consumer.invoker.fault.ListenerMethodNotFoundHandler;
import com.inomera.telco.commons.springkafka.consumer.invoker.fault.LoggingListenerMethodNotFoundHandler;
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
    private final ListenerMethodNotFoundHandler listenerMethodNotFoundHandler;

    public MethodInvoker(String groupId,
                         ListenerMethodRegistry listenerMethodRegistry,
                         List<ListenerInvocationInterceptor> interceptors) {
        this(groupId, listenerMethodRegistry, interceptors, new LoggingListenerMethodNotFoundHandler());
    }

    public MethodInvoker(String groupId,
                         ListenerMethodRegistry listenerMethodRegistry,
                         List<ListenerInvocationInterceptor> interceptors,
                         ListenerMethodNotFoundHandler listenerMethodNotFoundHandler) {
        this.groupId = groupId;
        this.listenerMethodRegistry = listenerMethodRegistry;
        this.interceptors = interceptors;
        this.listenerMethodNotFoundHandler = listenerMethodNotFoundHandler;
    }

    public FutureTask<ConsumerRecord<String, ?>> addRecord(final ConsumerRecord<String, ?> record) {
        return new FutureTask<>(() -> {
            try {
                final Object msg = record.value();
                if (msg == null) {
                    LOGGER.info("Null received from topic: {}", record.topic());
                    return;
                }

                final long invokerMethodCount = listenerMethodRegistry
                        .getListenerMethods(groupId, record.topic(), msg.getClass())
                        .peek(listenerMethod -> invokeListenerMethod(listenerMethod, msg))
                        .count();

                LOGGER.debug("Invoked {} listener methods", invokerMethodCount);
                if (invokerMethodCount == 0L) {
                    invokeListenerMethodNotFoundHandler(record);
                }
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

    private void invokeListenerMethodNotFoundHandler(ConsumerRecord<String, ?> record) {
        try {
            listenerMethodNotFoundHandler.onListenerMethodNotFound(groupId, record);
        } catch (Exception e) {
            // Swallow the exception, nothing should be thrown from here.
            LOGGER.error("Error calling listenerMethodNotFoundHandler: {}", e.getMessage(), e);
        }
    }
}
