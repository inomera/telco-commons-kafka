package com.inomera.telco.commons.example.domain.constant;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.GeneratedMessage;
import common.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import messaging.OrderMessage;
import messaging.PaymentMessage;
import player.PlayerInfoProto;
import player.PlayerRequestProto;
import player.PlayerResponseProto;
import player.command.PlayerCreateCommandProto;
import player.event.PlayerNotificationEventProto;
import todo.command.TodoUpdateCommandProto;
import todo.event.TodoInfoRequestEventProto;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DomainConstants {
    // Put all root classes (classes that will be serialized into the network)
    // into this map and give them unique ids.
    public static final Map<Class<? extends GeneratedMessage>, Integer> CLASS_IDS =
	    ImmutableMap.<Class<? extends GeneratedMessage>, Integer>builder()
		    // Example classes have negative ids
		    // and will be removed in the future.
		    // Real production classes should have positive ids
		    // and those ids can never change in the future.
		    .put(PlayerCreateCommandProto.class, 1)
		    .put(PlayerNotificationEventProto.class, 2)
		    .put(TodoUpdateCommandProto.class, 3)
		    .put(TodoInfoRequestEventProto.class, 4)
		    .put(BaseRestResponseProto.class, 5)
		    .put(ErrorRestResponseProto.class, 6)
		    .put(LongListProto.class, 7)
		    .put(LongStringMapProto.class, 8)
		    .put(ResponseStatusProto.class, 9)
		    .put(StringListProto.class, 10)
		    .put(StringLongMapProto.class, 11)
		    .put(StringStringMapProto.class, 12)
		    .put(PlayerInfoProto.class, 13)
		    .put(PlayerRequestProto.class, 14)
		    .put(PlayerResponseProto.class, 15)
		    .put(PaymentMessage.class, 16)
		    .put(OrderMessage.class, 17)
		    .build();
}
