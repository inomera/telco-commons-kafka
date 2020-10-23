package com.inomera.telco.commons.springkafka.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;

public final class HostUtils {

    private static final Logger LOG = LoggerFactory.getLogger(HostUtils.class);
    private static final String HOSTNAME_PARAM = "hostname";

    public static String getHostname() {
        String hostname = StringUtils.EMPTY;
        try {
            Process hostnameProcess = Runtime.getRuntime().exec(HOSTNAME_PARAM);

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(hostnameProcess.getInputStream()));
            while ((hostname = stdInput.readLine()) != null) {
                return hostname;
            }
        } catch (Exception e) {
            LOG.error("Hostname extract error :: {}", e.getMessage());
        } finally {
            if (StringUtils.isBlank(hostname)) {
                try {
                    return InetAddress.getLocalHost().getHostName();
                } catch (Exception e) {
                    //swallow exception
                }
            }
        }
        return hostname;
    }
}
