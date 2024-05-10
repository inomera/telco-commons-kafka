package com.inomera.telco.commons.example.springkafka.msg;

import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DomainConstants {
    // Put all root classes (classes that will be serialized into the network)
    // into this map and give them unique ids.
    public static final Map<Class<? extends Object>, Integer> CLASS_IDS =
	    ImmutableMap.<Class<? extends Object>, Integer>builder()
		    // Example classes have negative ids
		    // and will be removed in the future.
		    // Real production classes should have positive ids
		    // and those ids can never change in the future.
		    .put(SomethingHappenedConsumerMessage.class, 1)
		    .put(SomethingHappenedMessage.class, 2)
		    .put(SomethingHappenedBeautifullyMessage.class, 3)
		    .put(UnListenedMessage.class, 4)
		    .build();
}
