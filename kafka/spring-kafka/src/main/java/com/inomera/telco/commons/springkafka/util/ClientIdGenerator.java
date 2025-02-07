package com.inomera.telco.commons.springkafka.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.management.ManagementFactory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientIdGenerator {

    public static StringBuilder getContainerId(String id) {
        final StringBuilder sbContainerId = new StringBuilder();
        sbContainerId.append(id).append('-');
        sbContainerId.append(getNodeName());
        return sbContainerId;
    }

    private static String getNodeName() {
        String serverName = null;
        try {
            serverName = ManagementFactory.getRuntimeMXBean().getName();
        } catch (Exception ignored) {
        }

        if (serverName == null) {
            throw new RuntimeException("Can not determine server instance name.");
        }

        return serverName.replace('@', '-');
    }
}
