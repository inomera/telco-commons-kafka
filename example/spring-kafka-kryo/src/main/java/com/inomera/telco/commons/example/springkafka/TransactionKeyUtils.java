package com.inomera.telco.commons.example.springkafka;

import java.util.UUID;

public final class TransactionKeyUtils {

    public static String generateTxKey(){
	return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
