package com.inomera.telco.commons.springkafka.consumer.invoker;

import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import com.inomera.telco.commons.springkafka.consumer.invoker.fault.ListenerMethodNotFoundHandler;
import com.inomera.telco.commons.springkafka.consumer.invoker.fault.LoggingListenerMethodNotFoundHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

/**
 * @author Serdar Kuzucu
 */
public class MethodInvoker {
    private static final Logger LOG = LoggerFactory.getLogger(MethodInvoker.class);

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

    public FutureTask<InvokerResult> addRecord(final ConsumerRecord<String, ?> record) {
        final InvokerResult invokerResult = new InvokerResult(record);
        return new FutureTask<>(() -> {
            try {
                final Object msg = record.value();
                if (msg == null) {
                    LOG.info("Null received from topic: {}", record.topic());
                    return;
                }

                final long invokerMethodCount = listenerMethodRegistry
                        .getListenerMethods(groupId, record.topic(), msg.getClass())
                        .peek(listenerMethod -> { //DO NOT remove suggestion, invoke method inner method
                            final KafkaListener kafkaListener = invokeListenerMethod(listenerMethod, msg, record);
                            invokerResult.setKafkaListener(kafkaListener);
                        })
                        .count();

                LOG.trace("Invoked {} listener methods", invokerMethodCount);
                if (invokerMethodCount == 0L) {
                    invokeListenerMethodNotFoundHandler(record);
                }
            } catch (Exception e) {
                LOG.error("Error processing kafka message [{}].", record.value(), e);
            }
        }, invokerResult);
    }

    public FutureTask<BulkInvokerResult> addRecords(final Set<ConsumerRecord<String, ?>> records) {
        final ConsumerRecord<String, ?> firstRecord = records.iterator().next();
        final BulkInvokerResult invokerResult = new BulkInvokerResult(records);
        return new FutureTask<>(() -> {
            try {
                final Set<Object> messages = records.stream()
                        .map(ConsumerRecord::value)
                        .collect(Collectors.toSet());
                if (messages.isEmpty()) {
                    LOG.info("messages list is received as empty from topic: {}", firstRecord.topic());
                    return;
                }

                final long invokerMethodCount = listenerMethodRegistry
                        .getListenerMethods(groupId, firstRecord.topic(), Set.class)
                        .peek(listenerMethod -> { //DO NOT remove suggestion, invoke method inner method
                            final KafkaListener kafkaListener = invokeListenerMethods(listenerMethod, messages, firstRecord);
                            invokerResult.setKafkaListener(kafkaListener);
                        })
                        .count();

                LOG.trace("Invoked {} listener methods", invokerMethodCount);
                if (invokerMethodCount == 0L) {
                    invokeListenerMethodNotFoundHandler(firstRecord);
                }
            } catch (Exception e) {
                LOG.error("Error processing bulk kafka message [{}].", firstRecord.value(), e);
            }
        }, invokerResult);
    }

    private KafkaListener invokeListenerMethod(ListenerMethod listenerMethod, Object message, ConsumerRecord<String, ?> record) {
        try {
            invokeBeforeInterceptors(message, record.headers());
            return listenerMethod.invoke(message, record.topic());
        } catch (Exception e) {
            LOG.error("Error processing kafka message [{}]", message, e);
            return null;
        } finally {
            invokeAfterInterceptors(message, record.headers());
        }
    }

    private KafkaListener invokeListenerMethods(ListenerMethod listenerMethod, Set<Object> messages, ConsumerRecord<String, ?> record) {
        try {
            invokeBeforeInterceptors(messages, record.headers());
            return listenerMethod.invoke(messages, record.topic());
        } catch (Exception e) {
            LOG.error("Error processing kafka message [{}]", messages, e);
            return null;
        } finally {
            invokeAfterInterceptors(messages, record.headers());
        }
    }

    private void invokeBeforeInterceptors(Object message, Headers headers) {
        interceptors.forEach(interceptor -> interceptor.beforeInvocation(message, headers));
    }

    private void invokeBeforeInterceptors(Set<Object> messages, Headers headers) {
        interceptors.forEach(interceptor -> interceptor.beforeInvocation(messages, headers));
    }

    private void invokeAfterInterceptors(Object message, Headers headers) {
        interceptors.forEach(interceptor -> interceptor.afterInvocation(message, headers));
    }

    private void invokeAfterInterceptors(Set<Object> messages, Headers headers) {
        interceptors.forEach(interceptor -> interceptor.afterInvocation(messages, headers));
    }

    private void invokeListenerMethodNotFoundHandler(ConsumerRecord<String, ?> record) {
        try {
            listenerMethodNotFoundHandler.onListenerMethodNotFound(groupId, record);
        } catch (Exception e) {
            // Swallow the exception, nothing should be thrown from here.
            LOG.error("Error calling listenerMethodNotFoundHandler: {}", e.getMessage(), e);
        }
    }
}
