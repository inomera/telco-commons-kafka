package com.inomera.telco.commons.springkafka.consumer;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Serdar Kuzucu
 * @author Turgay Can
 */
public class PollerThreadStateChecker implements ThreadStateChecker {

    private static final Logger LOG = LoggerFactory.getLogger(PollerThreadStateChecker.class);

    private static ScheduledExecutorService executorService;

    private final ConsumerThreadStore threadStore;
    private final PollerThreadNotifier pollerThreadNotifier;

    public PollerThreadStateChecker(ConsumerThreadStore threadStore, Properties pollerThreadProperties) {
        this(threadStore, new DefaultPollerThreadNotifier(), pollerThreadProperties);
    }

    public PollerThreadStateChecker(ConsumerThreadStore threadStore, PollerThreadNotifier pollerThreadNotifier, Properties pollerThreadProperties) {
        this.threadStore = threadStore;
        this.pollerThreadNotifier = pollerThreadNotifier;
        final boolean checkerIsActive = BooleanUtils.toBoolean(pollerThreadProperties.getProperty("poller.thread.checker.active", "true"));
        if (!checkerIsActive) {
            LOG.info("Consumer Poller thread checker is not active");
            return;
        }
        executorService = Executors.newScheduledThreadPool(1);

        final int initialDelay = NumberUtils.toInt(pollerThreadProperties.getProperty("poller.thread.checker.initialDelaySec"), 30);
        final int retryInterval = NumberUtils.toInt(pollerThreadProperties.getProperty("poller.thread.checker.retryAsSec"), 30);
        executorService.scheduleAtFixedRate(this::check, initialDelay, retryInterval, TimeUnit.SECONDS);
    }

    @Override
    public void check() {
        final Set<PollerThreadState> monitoringThreads = new LinkedHashSet<>();
        for (Map.Entry<Long, PollerThreadState> threadEntry : threadStore.getThreads().entrySet()) {
            final String currentJvmState = getJvmState(threadEntry.getKey());
            final PollerThreadState pollerThreadState = threadEntry.getValue();
            pollerThreadState.setOldJvmState(pollerThreadState.getCurrentJvmState());
            pollerThreadState.setCurrentJvmState(currentJvmState);
            monitoringThreads.add(pollerThreadState);
        }

        for (PollerThreadState monitoringThread : monitoringThreads) {
            if (StringUtils.isNotBlank(monitoringThread.getCurrentJvmState()) && StringUtils.equals(monitoringThread.getCurrentJvmState(), monitoringThread.getOldJvmState())) {
                LOG.debug("PollerThreadStateChecker -> State is same :: {}", monitoringThread.toString());
                continue;
            }
            LOG.info("PollerThreadStateChecker -> State changed!! {}", monitoringThread.toString());
            if (StringUtils.isNotBlank(monitoringThread.getCurrentJvmState())) {
                continue;
            }
            LOG.warn("PollerThreadStateChecker -> Thread is dead!! {}", monitoringThread.toString());
            threadStore.getThreads().remove(monitoringThread.getThreadId());
            if (!monitoringThread.getConsumerPoller().shouldRestart()) {
                continue;
            }
            LOG.info("PollerThreadStateChecker -> Thread is trying to start!! {}", monitoringThread.toString());
            try {
                monitoringThread.getConsumerPoller().start();
            } catch (Exception e) {
                pollerThreadNotifier.alarm(monitoringThread.toString(), e);
                return;
            }
            LOG.info("PollerThreadStateChecker -> Thread started!! {}", monitoringThread.toString());
        }
    }

    private String getJvmState(Long threadId) {
        final Set<Thread> jvmThreads = Thread.getAllStackTraces().keySet();
        return jvmThreads.stream()
                .filter(jvmThread -> jvmThread.getId() == threadId)
                .findFirst()
                .map(jvmThread -> jvmThread.getState().name())
                .orElse(StringUtils.EMPTY);
    }

    @Override
    public void close() {
        if (executorService == null) {
            return;
        }
        if (executorService.isShutdown()) {
            return;
        }
        executorService.shutdown();
    }
}
