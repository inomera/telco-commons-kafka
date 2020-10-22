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
import java.util.concurrent.*;

public class PollerThreadStateChecker implements ThreadStateChecker {

    private static final Logger LOG = LoggerFactory.getLogger(PollerThreadStateChecker.class);

    private static ScheduledExecutorService executorService;

    private final ConsumerThreadStore threadStore;

    public PollerThreadStateChecker(ConsumerThreadStore threadStore, Properties pollerThreadProperties) {
        this.threadStore = threadStore;
        final boolean checkerIsActive = BooleanUtils.toBoolean(pollerThreadProperties.getProperty("poller.thread.checker.active", "true"));
        if (!checkerIsActive) {
            LOG.info("Consumer Poller thread checker is not active");
            return;
        }
        executorService = Executors.newScheduledThreadPool(1);

        final int initialDelay = NumberUtils.toInt(pollerThreadProperties.getProperty("poller.thread.checker.initialDelaySec"), 30);
        final int retryInterval = NumberUtils.toInt(pollerThreadProperties.getProperty("poller.thread.checker.retryAsSec"), 30);
        executorService.scheduleAtFixedRate(() -> check(), initialDelay, retryInterval, TimeUnit.SECONDS);
    }

    @Override
    public void check() {
        final Set<PollerThreadState> monitoringThreads = new LinkedHashSet<>();
        for (Map.Entry<Long, PollerThreadState> threadEntry : threadStore.getThreads().entrySet()) {
            final String currentJvmState = getJvmState(threadEntry.getKey());

            final PollerThreadState pollerThreadState = threadEntry.getValue();
            pollerThreadState.setCurrentJvmState(currentJvmState);
            monitoringThreads.add(pollerThreadState);
        }

        for (PollerThreadState monitoringThread : monitoringThreads) {
            if (StringUtils.equals(monitoringThread.getCurrentJvmState(), monitoringThread.getOldJvmState())) {
                LOG.info("PollerThreadStateChecker -> State is same \n {}", monitoringThread.toString());
                continue;
            }
            LOG.warn("PollerThreadStateChecker -> State changed!! \n {}", monitoringThread.toString());
            if(StringUtils.isBlank(monitoringThread.getCurrentJvmState())){
                LOG.warn("PollerThreadStateChecker -> Thread is dead!! \n {}", monitoringThread.toString());
                if(monitoringThread.getKafkaMessageConsumer().isAutoStartup()){
                    threadStore.getThreads().remove(monitoringThread.getThreadId());
                    LOG.warn("PollerThreadStateChecker -> Thread is trying to start!! \n {}", monitoringThread.toString());
                    monitoringThread.getKafkaMessageConsumer().start();
                    LOG.warn("PollerThreadStateChecker -> Thread started!! \n {}", monitoringThread.toString());
                    threadStore.getThreads().remove(monitoringThread.getThreadId());
                }
            }
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

}
