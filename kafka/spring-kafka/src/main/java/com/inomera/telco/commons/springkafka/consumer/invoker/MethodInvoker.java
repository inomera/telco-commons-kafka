package com.inomera.telco.commons.springkafka.consumer.invoker;

import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import com.inomera.telco.commons.springkafka.consumer.invoker.fault.ListenerMethodNotFoundHandler;
import com.inomera.telco.commons.springkafka.consumer.invoker.fault.LoggingListenerMethodNotFoundHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
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

                // An explicit loop, not peek()+count(): count() is allowed to skip the pipeline
                // when it can derive the size, so driving side effects from peek() only worked
                // by accident of there being a filter in front of it.
                final ListenerMethod[] listenerMethods =
                        listenerMethodRegistry.resolveListenerMethods(groupId, record.topic(), msg.getClass());

                for (ListenerMethod listenerMethod : listenerMethods) {
                    final KafkaListener kafkaListener = invokeListenerMethod(listenerMethod, msg, record);
                    if (kafkaListener != null) {
                        invokerResult.setKafkaListener(kafkaListener);
                    }
                }

                LOG.trace("Invoked {} listener methods", listenerMethods.length);
                if (listenerMethods.length == 0) {
                    invokeListenerMethodNotFoundHandler(record);
                }
            } catch (Exception e) {
                LOG.error("Error processing kafka message {}-{}@{}.",
                        record.topic(), record.partition(), record.offset(), e);
            }
        }, invokerResult);
    }

    public FutureTask<BulkInvokerResult> addRecords(final Set<ConsumerRecord<String, ?>> records) {
        final ConsumerRecord<String, ?> firstRecord = records.iterator().next();
        final BulkInvokerResult invokerResult = new BulkInvokerResult(records);
        return new FutureTask<>(() -> {
            try {
                // LinkedHashSet, not Collectors.toSet(): the listener contract is a Set, but at
                // least deliver the batch in offset order instead of HashMap order.
                final Set<Object> messages = records.stream()
                        .map(ConsumerRecord::value)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                if (messages.isEmpty()) {
                    LOG.info("messages list is received as empty from topic: {}", firstRecord.topic());
                    return;
                }
                if (messages.size() != records.size()) {
                    // Inherent to the Set-based listener signature; make it visible rather than
                    // silently handing the listener fewer messages than were polled.
                    LOG.warn("Bulk batch for topic {} collapsed from {} records to {} distinct payloads.",
                            firstRecord.topic(), records.size(), messages.size());
                }

                final ListenerMethod[] listenerMethods =
                        listenerMethodRegistry.resolveListenerMethods(groupId, firstRecord.topic(), Set.class);

                for (ListenerMethod listenerMethod : listenerMethods) {
                    final KafkaListener kafkaListener = invokeListenerMethods(listenerMethod, messages, firstRecord);
                    if (kafkaListener != null) {
                        invokerResult.setKafkaListener(kafkaListener);
                    }
                }

                LOG.trace("Invoked {} listener methods", listenerMethods.length);
                if (listenerMethods.length == 0) {
                    invokeListenerMethodNotFoundHandler(firstRecord);
                }
            } catch (Exception e) {
                LOG.error("Error processing bulk kafka batch for topic {}.", firstRecord.topic(), e);
            }
        }, invokerResult);
    }

    private KafkaListener invokeListenerMethod(ListenerMethod listenerMethod, Object message, ConsumerRecord<String, ?> record) {
        try {
            invokeBeforeInterceptors(message, record.headers());
            return listenerMethod.invoke(message, record.topic());
        } catch (Exception e) {
            LOG.error("Error processing kafka message {}-{}@{}",
                    record.topic(), record.partition(), record.offset(), e);
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
            LOG.error("Error processing kafka batch of {} messages from topic {}",
                    messages.size(), record.topic(), e);
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
