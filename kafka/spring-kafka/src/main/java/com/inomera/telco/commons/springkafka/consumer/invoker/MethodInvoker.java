package com.inomera.telco.commons.springkafka.consumer.invoker;

import com.inomera.telco.commons.springkafka.annotation.KafkaListener;
import com.inomera.telco.commons.springkafka.consumer.invoker.fault.ListenerMethodNotFoundHandler;
import com.inomera.telco.commons.springkafka.consumer.invoker.fault.LoggingListenerMethodNotFoundHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
			.peek(listenerMethod -> {
			    final KafkaListener kafkaListener = invokeListenerMethod(listenerMethod, msg, record.topic());
			    invokerResult.setKafkaListener(kafkaListener);
			})
			.count();

		LOG.debug("Invoked {} listener methods", invokerMethodCount);
		if (invokerMethodCount == 0L) {
		    invokeListenerMethodNotFoundHandler(record);
		}
	    } catch (Exception e) {
		LOG.error("Error processing kafka message [{}].", record.value(), e);
	    }
	}, invokerResult);
    }

    public FutureTask<InvokerResult> addRecords(final Set<ConsumerRecord<String, ?>> records) {
	final ConsumerRecord<String, ?> firstRecord = records.iterator().next();
	final InvokerResult invokerResult = new InvokerResult(firstRecord);
	return new FutureTask<>(() -> {
	    try {
		final Set<Object> messages = records.stream().map(r -> r.value()).collect(Collectors.toSet());
		if (messages.isEmpty()) {
		    LOG.info("messages list is received as empty from topic: {}", firstRecord.topic());
		    return;
		}

		final long invokerMethodCount = listenerMethodRegistry
			.getListenerMethods(groupId, firstRecord.topic(), Set.class)
			.peek(listenerMethod -> {
			    final KafkaListener kafkaListener = invokeListenerMethods(listenerMethod, messages, firstRecord.topic());
			    invokerResult.setKafkaListener(kafkaListener);
			})
			.count();

		LOG.debug("Invoked {} listener methods", invokerMethodCount);
		if (invokerMethodCount == 0L) {
		    invokeListenerMethodNotFoundHandler(firstRecord);
		}
	    } catch (Exception e) {
		LOG.error("Error processing kafka message [{}].", firstRecord.value(), e);
	    }
	}, invokerResult);
    }

    private KafkaListener invokeListenerMethod(ListenerMethod listenerMethod, Object message, String topic) {
	try {
	    invokeBeforeInterceptors(message);
	    return listenerMethod.invoke(message, topic);
	} catch (Exception e) {
	    LOG.error("Error processing kafka message [{}]", message, e);
	    return null;
	} finally {
	    invokeAfterInterceptors(message);
	}
    }

    private KafkaListener invokeListenerMethods(ListenerMethod listenerMethod, Set<Object> messages, String topic) {
	try {
	    invokeBeforeInterceptors(messages);
	    return listenerMethod.invoke(messages, topic);
	} catch (Exception e) {
	    LOG.error("Error processing kafka message [{}]", messages, e);
	    return null;
	} finally {
	    invokeAfterInterceptors(messages);
	}
    }

    private void invokeBeforeInterceptors(Object message) {
	interceptors.forEach(interceptor -> interceptor.beforeInvocation(message));
    }

    private void invokeBeforeInterceptors(Set<Object> messages) {
	interceptors.forEach(interceptor -> interceptor.beforeInvocation(messages));
    }

    private void invokeAfterInterceptors(Object message) {
	interceptors.forEach(interceptor -> interceptor.afterInvocation(message));
    }

    private void invokeAfterInterceptors(Set<Object> messages) {
	interceptors.forEach(interceptor -> interceptor.afterInvocation(messages));
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
