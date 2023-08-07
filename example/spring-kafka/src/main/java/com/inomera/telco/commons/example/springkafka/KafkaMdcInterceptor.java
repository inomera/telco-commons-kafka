package com.inomera.telco.commons.example.springkafka;

import com.inomera.telco.commons.springkafka.consumer.invoker.ListenerInvocationInterceptor;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

public class KafkaMdcInterceptor implements ListenerInvocationInterceptor {
    public static final String LOG_TRACK_KEY_FIELD = "logTrackId";

    @Override
    public void beforeInvocation(Object incomingMessage, Headers headers) {
	final String logTrackKey = getOrGenerateLogTrackId(headers);
	MdcUtils.setLogTrackKey(logTrackKey);
    }

    @Override
    public void afterInvocation(Object message, Headers headers) {
	MdcUtils.removeLogTrackKey();
    }

    private String getOrGenerateLogTrackId(Headers headers) {
	final Iterable<Header> logHeaders = headers.headers(LOG_TRACK_KEY_FIELD);
	if (logHeaders.iterator().hasNext()) {
	    final Header logTrackHeader = logHeaders.iterator().next();
	    final Object logTrackKey = SerializationUtils.deserialize(logTrackHeader.value());
	    return (String) logTrackKey;
	}
	return MdcUtils.generateNewLogTrackKey();
    }

}
