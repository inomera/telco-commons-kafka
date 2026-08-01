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

    // Instance-scoped: a static field meant a second checker silently leaked the first one's
    // scheduler and close() only ever shut down the most recently constructed instance.
    private ScheduledExecutorService executorService;

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
            final PollerThreadState pollerThreadState = threadEntry.getValue();
            final String currentJvmState = getJvmState(pollerThreadState);
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

    /**
     * Reads the thread's state from the stored reference.
     *
     * <p>This used to call {@code Thread.getAllStackTraces()} once per monitored thread, which
     * captures a full stack dump of every thread in the JVM — O(threads&sup2;) work on a timer —
     * and never reports virtual threads, so a virtual poller would always look dead and be
     * restarted forever.
     */
    private String getJvmState(PollerThreadState state) {
        final Thread thread = state.getThread();
        if (thread == null || !thread.isAlive()) {
            return StringUtils.EMPTY;
        }
        return thread.getState().name();
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
