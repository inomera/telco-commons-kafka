package com.inomera.telco.commons.springkafka.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Resolves the local hostname once, at class initialization.
 *
 * <p>This used to shell out with {@code Runtime.getRuntime().exec("hostname")} on every call.
 * That forked a process per poller thread creation, never closed the process or its streams
 * (leaking file descriptors and leaving zombies behind), returned from a {@code finally} block —
 * silently discarding the value computed in {@code try} — and failed outright in images with no
 * {@code hostname} binary. {@code Runtime.exec(String)} has also been deprecated since JDK 18.
 *
 * @author Turgay Can
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HostUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HostUtils.class);

    private static final String HOSTNAME = resolveHostname();

    public static String getHostname() {
        return HOSTNAME;
    }

    private static String resolveHostname() {
        // Containers export this; it is the cheapest and most reliable source.
        final String fromEnv = System.getenv("HOSTNAME");
        if (StringUtils.isNotBlank(fromEnv)) {
            return fromEnv;
        }
        try {
            final String fromInet = InetAddress.getLocalHost().getHostName();
            if (StringUtils.isNotBlank(fromInet)) {
                return fromInet;
            }
        } catch (UnknownHostException e) {
            LOG.debug("Could not resolve local hostname: {}", e.getMessage());
        }
        return "unknown-" + ProcessHandle.current().pid();
    }
}
