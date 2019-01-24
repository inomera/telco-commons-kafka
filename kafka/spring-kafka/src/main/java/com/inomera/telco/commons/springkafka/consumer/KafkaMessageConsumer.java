package com.inomera.telco.commons.springkafka.consumer;

import com.inomera.telco.commons.lang.PropertyUtils;
import com.inomera.telco.commons.lang.thread.FutureUtils;
import com.inomera.telco.commons.lang.thread.ThreadUtils;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.Ordered;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Serdar Kuzucu
 */
public class KafkaMessageConsumer implements SmartLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMessageConsumer.class);
    private volatile boolean running = false;

    private Map<TopicPartition, OffsetAndMetadata> offsetMap = new HashMap<>(1);
    private ConsumerListener consumerListener;
    private ListenerInvoker[] invokers;
    private ExecutorService executorService;

    private KafkaConsumerProperties kafkaConsumerProperties;
    private ListenerMethodRegistry listenerMethodRegistry;
    private List<ListenerInvocationInterceptor> interceptors;
    private ThreadFactory invokerThreadFactory;
    private Deserializer<?> valueDeserializer;
    private int numberOfInvokerThreads;

    KafkaMessageConsumer(KafkaConsumerProperties kafkaConsumerProperties,
                         ListenerMethodRegistry listenerMethodRegistry, List<ListenerInvocationInterceptor> interceptors,
                         ThreadFactory consumerThreadFactory, ThreadFactory invokerThreadFactory, Deserializer<?> valueDeserializer,
                         int numberOfInvokerThreads) {
        this.kafkaConsumerProperties = kafkaConsumerProperties;
        this.listenerMethodRegistry = listenerMethodRegistry;
        this.interceptors = interceptors;
        this.invokerThreadFactory = invokerThreadFactory;
        this.valueDeserializer = valueDeserializer;
        this.numberOfInvokerThreads = numberOfInvokerThreads;
        this.executorService = Executors.newCachedThreadPool(consumerThreadFactory);
    }

    @Override
    public void start() {
        startConsume();
        running = true;
    }

    @Override
    public void stop() {
        stopConsume();
        if (executorService != null) {
            executorService.shutdownNow();
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void stop(Runnable callback) {
        this.stop();
        callback.run();
    }

    private void startConsume() {
        LOGGER.info("Starting consumer with properties {}", kafkaConsumerProperties);

        consumerListener = new ConsumerListener(
                new KafkaConsumer<>(buildConsumerProperties(), new StringDeserializer(), valueDeserializer));
        invokers = new ListenerInvoker[numberOfInvokerThreads];
        for (int i = 0; i < numberOfInvokerThreads; i++) {
            invokers[i] = new ListenerInvoker();

            final Thread invokerThread = invokerThreadFactory.newThread(invokers[i]);
            invokerThread.start();
        }

        executorService.submit(consumerListener);
    }

    private void stopConsume() {
        if (consumerListener != null) {
            consumerListener.stopConsume();
        }

        if (invokers != null) {
            for (ListenerInvoker th : invokers) {
                th.stopGracefully();
            }
        }
    }

    private Properties buildConsumerProperties() {
        final Properties props = PropertyUtils.copyProperties(kafkaConsumerProperties.getKafkaConsumerProperties());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConsumerProperties.getGroupId());
        if (kafkaConsumerProperties.hasPatternBasedTopic()) {
            /*
             * We are decreasing the default metadata load period, which is 5
             * minutes, to 30s so that pattern based subscriptions take affect
             * shorter.
             */
            props.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, "30000");
        }
        props.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, kafkaConsumerProperties.getClientId());
        return PropertyUtils.overrideWithSystemArguments(props);
    }

    private class ConsumerListener implements ConsumerRebalanceListener, Runnable {
        private Consumer<String, ?> consumer;
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private Map<TopicPartition, List<Future<ConsumerRecord<String, ?>>>> inProgressMessages = new HashMap<>();

        ConsumerListener(Consumer<String, ?> consumer) {
            this.consumer = consumer;
        }

        void stopConsume() {
            if (running.get()) {
                running.set(false);
                consumer.wakeup();
                do {
                    ThreadUtils.sleepQuietly(500);
                } while (!closed.get());
            }
        }

        @Override
        public void run() {
            try {
                if (kafkaConsumerProperties.hasPatternBasedTopic()) {
                    consumer.subscribe(kafkaConsumerProperties.getTopicPattern(), this);
                } else {
                    consumer.subscribe(kafkaConsumerProperties.getTopics(), this);
                }

                List<Future<ConsumerRecord<String, ?>>> partitionFutures;

                Collection<TopicPartition> toBePause = new HashSet<>();
                Collection<TopicPartition> toBeResume = new HashSet<>();
                TopicPartition tp;
                int pollWaitMs = 3000;
                pollLoop:
                while (running.get()) {
                    final ConsumerRecords<String, ?> records = consumer.poll(pollWaitMs);

                    if (!records.isEmpty()) {
                        pollWaitMs = 0;
                        if (kafkaConsumerProperties.isAtMostOnceBulk() && !commitLastOffsets()) {
                            continue;
                        }

                        toBePause.clear();
                        for (ConsumerRecord<String, ?> rec : records) {
                            if (kafkaConsumerProperties.isAtMostOnceSingle() && !commitOffset(rec)) {
                                continue pollLoop;
                            }

                            tp = new TopicPartition(rec.topic(), rec.partition());
                            partitionFutures = inProgressMessages.get(tp);
                            try {
                                partitionFutures.add(sendToInvoker(rec));
                                toBePause.add(new TopicPartition(rec.topic(), rec.partition()));
                            } catch (Exception e) {
                                LOGGER.error("Error processing kafka message [{}].", rec.value(), e);
                            }
                        }

                        // do not read any records on the next poll cycle
                        LOGGER.debug("PAUSED-> {}", toBePause);
                        consumer.pause(toBePause);
                    } else {
                        // increase poll wait time until 3sec
                        pollWaitMs = Math.min(pollWaitMs + 10, 3000);
                    }

                    toBeResume.clear();

                    for (Map.Entry<TopicPartition, List<Future<ConsumerRecord<String, ?>>>> e : inProgressMessages.entrySet()) {
                        if (!e.getValue().isEmpty()) {
                            ConsumerRecord<String, ?> lastCompleted = null;
                            ConsumerRecord<String, ?> crTemp;
                            for (Iterator<Future<ConsumerRecord<String, ?>>> currentRecord = e.getValue().iterator(); currentRecord.hasNext(); ) {
                                Future<ConsumerRecord<String, ?>> nextRecord = currentRecord.next();
                                if (nextRecord.isDone()) {
                                    if (lastCompleted == null) {
                                        lastCompleted = FutureUtils.getUnchecked(nextRecord);
                                    } else if (lastCompleted.offset() < (crTemp = FutureUtils.getUnchecked(
                                            nextRecord)).offset()) {
                                        lastCompleted = crTemp;
                                    }
                                    currentRecord.remove();
                                } else {
                                    break;
                                }
                            }

                            if (lastCompleted != null && kafkaConsumerProperties.isAtLeastOnceSingle()) {
                                commitOffset(lastCompleted);
                            }

                            if (e.getValue().isEmpty()) {
                                if (kafkaConsumerProperties.isAtLeastOnceBulk()) {
                                    if (lastCompleted != null) {
                                        commitOffset(lastCompleted);
                                    }
                                }
                                toBeResume.add(e.getKey());
                            }

                        }
                    }

                    if (!toBeResume.isEmpty()) {
                        consumer.resume(toBeResume);
                        LOGGER.debug("RESUMED-> {}", toBePause);
                    }
                }
            } catch (WakeupException e) {
                LOGGER.info("WakeupException is handled. Shutting down the consumer.");
                if (running.get()) {
                    LOGGER.error("WakeupException occurred while running=true! " +
                            "This may be an error therefore here is the stacktrace for error {}", e.getMessage(), e);
                    throw e;
                }
            } catch (Exception e) {
                LOGGER.error("Exception exception occurred when polling or committing, message : {}",
                        e.getMessage(), e);
            } finally {
                consumer.close();
            }

            closed.set(true);
        }

        private Future<ConsumerRecord<String, ?>> sendToInvoker(final ConsumerRecord<String, ?> rec) {
            int workerIndex = (Math.abs(rec.topic().hashCode()) + rec.partition()) % invokers.length;
            return invokers[workerIndex].addRecord(rec);
        }

        private boolean commitLastOffsets() {
            try {
                consumer.commitSync();
                return true;
            } catch (CommitFailedException e) {
                LOGGER.info("Committing last offsets failed for {}.", kafkaConsumerProperties.getClientId(), e);
                return false;
            }
        }

        private boolean commitOffset(ConsumerRecord<String, ?> rec) {
            try {
                offsetMap.clear();
                offsetMap.put(new TopicPartition(rec.topic(), rec.partition()),
                        new OffsetAndMetadata(rec.offset() + 1));
                consumer.commitSync(offsetMap);
                return true;
            } catch (CommitFailedException e) {
                LOGGER.error("Offset commit failed for {} . offset={}, request={}",
                        kafkaConsumerProperties.getClientId(), offsetMap, rec.value(), e);
                return false;
            }
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            inProgressMessages.clear();
            for (TopicPartition tp : partitions) {
                inProgressMessages.put(tp, new ArrayList<>());
            }

            LOGGER.info("ASSIGNED-> {}", partitions);
        }

        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitionsRevoked) {
            if (!(kafkaConsumerProperties.isAtLeastOnce())) {
                return;
            }

            for (TopicPartition tpRevoked : partitionsRevoked) {
                List<Future<ConsumerRecord<String, ?>>> inProgressTasks = inProgressMessages.get(tpRevoked);
                if (inProgressTasks != null && !inProgressTasks.isEmpty()) {
                    for (int i = inProgressTasks.size() - 1; i >= 0; --i) {
                        Future<ConsumerRecord<String, ?>> f = inProgressTasks.get(i);
                        if (f.isDone()) {
                            commitOffset(FutureUtils.getUnchecked(f));
                            break;
                        }
                    }
                }
            }
            LOGGER.info("REVOKED-> {}", partitionsRevoked);
        }

    }

    private class ListenerInvoker implements Runnable {
        private BlockingQueue<FutureTask<ConsumerRecord<String, ?>>> taskQueue;
        private volatile boolean running = true;

        ListenerInvoker() {
            taskQueue = new LinkedBlockingQueue<>();
        }

        void stopGracefully() {
            // TODO: handle incomplete massages
            running = false;
        }

        FutureTask<ConsumerRecord<String, ?>> addRecord(final ConsumerRecord<String, ?> rec) {
            final FutureTask<ConsumerRecord<String, ?>> futureTask = new FutureTask<>(() -> {
                try {
                    final Object msg = rec.value();
                    final ListenerEndpointDescriptor listenerEndpointDescriptor = new ListenerEndpointDescriptor(
                            rec.topic(), kafkaConsumerProperties.getGroupId(), msg.getClass());
                    final Collection<ListenerMethod> listenerMethods = listenerMethodRegistry.getListenerMethods(
                            listenerEndpointDescriptor);

                    listenerMethods.forEach(listenerMethod -> invokeListenerMethod(listenerMethod, msg));
                } catch (Exception e) {
                    LOGGER.error("Error processing kafka message [{}].", rec.value(), e);
                }
            }, rec);

            taskQueue.add(futureTask);

            return futureTask;
        }

        private void invokeListenerMethod(ListenerMethod listenerMethod, Object message) {
            try {
                invokeBeforeInterceptors(message);
                listenerMethod.invoke(message);
            } catch (Exception e) {
                LOGGER.error("Error processing kafka message [{}]", message, e);
            } finally {
                invokeAfterInterceptors(message);
            }
        }

        private void invokeBeforeInterceptors(Object message) {
            interceptors.forEach(interceptor -> interceptor.beforeInvocation(message));
        }

        private void invokeAfterInterceptors(Object message) {
            interceptors.forEach(interceptor -> interceptor.afterInvocation(message));
        }

        @Override
        public void run() {
            while (running) {
                try {
                    final FutureTask<ConsumerRecord<String, ?>> task = taskQueue.poll(2, TimeUnit.SECONDS);
                    if (task != null) {
                        task.run();
                    }
                } catch (InterruptedException e) {
                    LOGGER.info("ListenerInvoker thread is interrupted. Closing...");
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }

            LOGGER.info("ListenerInvoker is shut down.");
        }
    }
}
