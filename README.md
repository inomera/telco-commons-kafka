# About This Repo

To publish a version to maven repository, 
you should edit your local gradle.properties file.

The file is: `/path-to-user-home/.gradle/gradle.properties`

For example: `~/.gradle/gradle.properties`

Add credentials for nexus repository to `gradle.properties` file.

Example `gradle.properties` file:

```
telcoTeamUsername=********
telcoTeamPassword=************************
```

Then execute `gradle` `publish` task on the project.

For example, to publish the project `spring-kafka`, 
you need to execute the following command in project root:

```
gradle :spring-kafka:publish
``` 

The repository will not allow you to publish the same version twice.
You need to change version of the artifact every time you want to publish.

You can change version in `build.gradle` file of the sub-project.

```
build.gradle > publishing > publications > mavenJava > version
```

Please change the version wisely.

# How to Use Kafka

Autowire `KafkaConsumerBuilder` wherever you want to build a new kafka consumer.

## Ordered Consumer

Threads share messages by their partition key.

```java
@Bean
public KafkaMessageConsumer consumer(KafkaConsumerBuilder builder) throws IOException {
    final Properties properties = new Properties();
    properties.load(new StringReader("enable.auto.commit=false\n" +
            "auto.commit.interval.ms=2147483647\n" +
            "bootstrap.servers=localhost:9092\n" +
            "heartbeat.interval.ms=10000\n" +
            "request.timeout.ms=31000\n" +
            "session.timeout.ms=30000\n" +
            "max.partition.fetch.bytes=15728640\n" +
            "max.poll.records=10\n" +
            "auto.offset.reset=earliest\n" +
            "metadata.max.age.ms=10000"));

    return builder.properties(properties)
            .groupId("notification_mo")
            .topicPattern(Pattern.compile("simpl\\.event\\..*"))
            .offsetCommitStrategy(OffsetCommitStrategy.AT_MOST_ONCE_SINGLE)
            .valueDeserializer(kafkaDeserializer())
            .autoPartitionPause(true)
            .invoker()
            .interceptor(myInterceptor())
            .ordered()
            .numberOfThreads(3)
            .and()
            .and()
            .build();
}
```

## Unordered Consumers

### Single Thread Pool

Order is not guaranteed. 
Single thread pool consumes all messages coming from all topics listened by the consumer.

```java
@Bean
public KafkaMessageConsumer consumer(KafkaConsumerBuilder builder) throws IOException {
    final Properties properties = new Properties();
    properties.load(new StringReader("enable.auto.commit=false\n" +
            "auto.commit.interval.ms=2147483647\n" +
            "bootstrap.servers=localhost:9092\n" +
            "heartbeat.interval.ms=10000\n" +
            "request.timeout.ms=31000\n" +
            "session.timeout.ms=30000\n" +
            "max.partition.fetch.bytes=15728640\n" +
            "max.poll.records=10\n" +
            "auto.offset.reset=earliest\n" +
            "metadata.max.age.ms=10000"));

    return builder.properties(properties)
            .groupId("notification_mo")
            .topicPattern(Pattern.compile("simpl\\.event\\..*"))
            .offsetCommitStrategy(OffsetCommitStrategy.AT_MOST_ONCE_SINGLE)
            .valueDeserializer(kafkaDeserializer())
            .autoPartitionPause(false)
            .invoker()
            .interceptor(myInterceptor())
            .unordered()
            .singleExecutor()
            .coreThreadCount(1)
            .maxThreadCount(10)
            .keepAliveTime(1)
            .keepAliveTimeUnit(TimeUnit.MINUTES)
            .queueCapacity(0)
            .and()
            .and()
            .and()
            .build();
}
```

### Thread Pool Executor Per Topic

Creates a new ThreadPoolExecutor per topic.
Uses the same configuration (thread counts, keep alive times) for all executors.

```java
@Bean
public KafkaMessageConsumer consumer(KafkaConsumerBuilder builder) throws IOException {
    final Properties properties = new Properties();
    properties.load(new StringReader("enable.auto.commit=false\n" +
            "auto.commit.interval.ms=2147483647\n" +
            "bootstrap.servers=localhost:9092\n" +
            "heartbeat.interval.ms=10000\n" +
            "request.timeout.ms=31000\n" +
            "session.timeout.ms=30000\n" +
            "max.partition.fetch.bytes=15728640\n" +
            "max.poll.records=10\n" +
            "auto.offset.reset=earliest\n" +
            "metadata.max.age.ms=10000"));

    return builder.properties(properties)
            .groupId("notification_mo")
            .topicPattern(Pattern.compile("simpl\\.event\\..*"))
            .offsetCommitStrategy(OffsetCommitStrategy.AT_MOST_ONCE_SINGLE)
            .valueDeserializer(kafkaDeserializer())
            .autoPartitionPause(false)
            .invoker()
            .interceptor(myInterceptor())
            .unordered()
            .executorPerTopic()
            .coreThreadCount(1)
            .maxThreadCount(10)
            .keepAliveTime(1)
            .keepAliveTimeUnit(TimeUnit.MINUTES)
            .queueCapacity(0)
            .and()
            .and()
            .and()
            .build();
}
```

### Dynamic Executor Mapping

Allows user to configure different thread pool executors per message.

```java
private Function<ConsumerRecord<String, ?>, String> topicNameToPartnerKeyFunction() {
    return record -> {
        // Topic names: simpl.notif.PTRINOMERA, simpl.notif.PTRCETECH
        // Executor name is partner key
        final String topicName = record.topic();
        return StringUtils.substringAfterLast(topicName, ".");
    };
}

@Bean
public KafkaMessageConsumer consumer(KafkaConsumerBuilder builder) throws IOException {
    final Properties properties = new Properties();
    properties.load(new StringReader("enable.auto.commit=false\n" +
            "auto.commit.interval.ms=2147483647\n" +
            "bootstrap.servers=localhost:9092\n" +
            "heartbeat.interval.ms=10000\n" +
            "request.timeout.ms=31000\n" +
            "session.timeout.ms=30000\n" +
            "max.partition.fetch.bytes=15728640\n" +
            "max.poll.records=10\n" +
            "auto.offset.reset=earliest\n" +
            "metadata.max.age.ms=10000"));

    return builder.properties(properties)
            .groupId("notification_consumer")
            .topicPattern(Pattern.compile("simpl\\.notif\\..*"))
            .offsetCommitStrategy(OffsetCommitStrategy.AT_MOST_ONCE_SINGLE)
            .valueDeserializer(kafkaDeserializer())
            .invoker()
            .unordered()
            .dynamicNamedExecutors()
            .executorNamingFunction(topicNameToPartnerKeyFunction())
            .configureDefaultExecutor(1, 1, 1, TimeUnit.MINUTES)
            .configureExecutor("PTRINOMERA", 2, 2, 1, TimeUnit.MINUTES)
            .configureExecutor("PTRCETECH", 1, 1, 1, TimeUnit.MINUTES)
            .queueCapacity(0)
            .and()
            .and()
            .interceptor(myInterceptor())
            .and()
            .autoPartitionPause(false)
            .build();
}
```


### Custom Executor Mapping

Allows user to configure different thread pool executors per message.

```java
private Function<ConsumerRecord<String, ?>, String> topicNameToPartnerKeyFunction() {
    return record -> {
        // Topic names: simpl.notif.PTRINOMERA, simpl.notif.PTRCETECH
        // Executor name is partner key
        final String topicName = record.topic();
        return StringUtils.substringAfterLast(topicName, ".");
    };
}

@Bean
public DynamicNamedExecutorStrategy notificationConsumerExecutorStrategy() {
    return new DynamicNamedExecutorStrategy(new ThreadPoolExecutorSpec(1, 5, 1, TimeUnit.MINUTES, new IncrementalNamingThreadFactory("def-exec-"),
            SynchronousQueue::new), topicNameToPartnerKeyFunction());
}

@Bean
public KafkaMessageConsumer consumer(KafkaConsumerBuilder builder) throws IOException {
    final Properties properties = new Properties();
    properties.load(new StringReader("enable.auto.commit=false\n" +
            "auto.commit.interval.ms=2147483647\n" +
            "bootstrap.servers=localhost:9092\n" +
            "heartbeat.interval.ms=10000\n" +
            "request.timeout.ms=31000\n" +
            "session.timeout.ms=30000\n" +
            "max.partition.fetch.bytes=15728640\n" +
            "max.poll.records=10\n" +
            "auto.offset.reset=earliest\n" +
            "metadata.max.age.ms=10000"));

    return builder.properties(properties)
            .groupId("notification_mo")
            .topicPattern(Pattern.compile("simpl\\.event\\..*"))
            .offsetCommitStrategy(OffsetCommitStrategy.AT_MOST_ONCE_SINGLE)
            .valueDeserializer(kafkaDeserializer())
            .invoker()
            .unordered()
            .custom(notificationConsumerExecutorStrategy())
            .interceptor(myInterceptor())
            .and()
            .autoPartitionPause(false)
            .build();
}
```

### Bulk Message Consumer 

Supports all features of the single message consumer

```java
@Bean("bulkConsumer")
public KafkaMessageConsumer bulkConsumer(KafkaConsumerBuilder builder) {

final Properties properties = new Properties();
	properties.load(new StringReader("enable.auto.commit=false\n" +
	"auto.commit.interval.ms=2147483647\n" +
	"bootstrap.servers=localhost:9092\n" +
	"heartbeat.interval.ms=10000\n" +
	"request.timeout.ms=31000\n" +
	"session.timeout.ms=30000\n" +
	"max.partition.fetch.bytes=15728640\n" +
	"max.poll.records=10\n" +
	"auto.offset.reset=earliest\n" +
	"metadata.max.age.ms=10000"));
	
        return builder
                .properties(properties)
                .groupId("bulk-event-logger")
                .topics("mouse-bulk-event.click", "mouse-bulk-event.dblclick", "bulk-example.unlistened-topic")
                .offsetCommitStrategy(defaultKafkaConsumerConfigurationProperties.getOffsetCommitStrategy())
                .valueDeserializer(kafkaDeserializer())
                .autoPartitionPause(true)
                .invoker()
                .unordered()
                .dynamicNamedExecutors()
                .configureExecutor("mouse-bulk-event.click", 3, 5, 1, TimeUnit.MINUTES)
                .configureExecutor("mouse-bulk-event.dblclick", 3, 5, 1, TimeUnit.MINUTES)
                .configureExecutor("bulk-example.unlistened-topic", 3, 5, 1, TimeUnit.MINUTES)
                .and()
                .and()
                .and()
                .threadStore(consumerThreadStore())
                .buildBulk();
    }
```

### KafkaListener

Sample usage of KafkaListener single message

```java
@KafkaListener(groupId = "event-logger", topics = {"mouse-event.click", "mouse-event.dblclick"})
public void handle(Message message) {
LOG.info("handle : message={}", message);
ThreadUtils.sleepQuietly(300);
if (message instanceof SomethingHappenedConsumerMessage) {
final SomethingHappenedConsumerMessage msg = (SomethingHappenedConsumerMessage) message;
if (msg.getTime() % 2 == 0) {
LOG.warn("Commit key={}, msg={}", msg.getTxKey(), msg);
return;
}
throw new RuntimeException("retry test single message consumer without retry");
}
}
```

Sample usage of KafkaListener bulk message without retry

```java
    @KafkaListener(groupId = "bulk-event-logger", topics = {"mouse-bulk-event.click"}, includeSubclasses = true, retry = NONE)
    public void bulkHandleClick(Set<AbstractMessage> messages) {
	final Message message = messages.iterator().next();
	LOG.info("handle : message={}, messageCount={}", message, messages.size());
	ThreadUtils.sleepQuietly(300);
	if (message instanceof SomethingHappenedConsumerMessage) {
	    final SomethingHappenedConsumerMessage msg = (SomethingHappenedConsumerMessage) message;
	    if (msg.getTime() % 2 == 0) {
		LOG.warn("Commit key={}, msg={}", msg.getTxKey(), msg);
		return;
	    }
	    throw new RuntimeException("retry test bulk message consumer without retry");
	}
    }
```

Sample usage of KafkaListener bulk message with in memory retry

```java

    @KafkaListener(groupId = "retry-bulk-event-logger", topics = {"mouse-bulk-event.dblclick"}, includeSubclasses = true, retry = RETRY_IN_MEMORY_TASK, retryCount = 3)
    public void bulkHandleInMemoryDoubleClick(Set<AbstractMessage> messages) {
	final Message message = messages.iterator().next();
	LOG.info("handle : message={}, messageCount={}", message, messages.size());

	ThreadUtils.sleepQuietly(5);
	if (message instanceof SomethingHappenedConsumerMessage) {
	    final SomethingHappenedConsumerMessage msg = (SomethingHappenedConsumerMessage) message;
	    if (msg.getTime() % 2 == 0) {
		LOG.info("Commit key={}, msg={}", msg.getTxKey(), msg);
		return;
	    }
	    throw new RuntimeException("retry test bulk message with in memory retry");
	}
    }

```


```java
/*
Topic names
*/
String[] topics() default {};

/*
Consumer group id value
*/
String groupId() default "";

/*
subscribe the child messages of the parent message
 */
boolean includeSubclasses() default false;

/*
retry count value. It works for RETRY strategies(excludes NONE)
 */
int retryCount() default 3;

/*
Retry policy works only for below message commit (ack) strategies.
Default value is NONE.
com.inomera.telco.commons.springkafka.consumer.OffsetCommitStrategy
AT_LEAST_ONCE_ONCE
AT_LEAST_ONCE_BULK
*/
RETRY retry() default RETRY.NONE;

/*
NONE : no retry.
RETRY_FROM_BROKER : do not ack/commit message to broker! re-start consumer, consumer polls message from broker again. default retryCount val is 3.
RETRY_IN_MEMORY_TASK : commit/ack message to broker, retry in consumer local queue. default retryCount val is 3.
 */
enum RETRY {
NONE,
RETRY_FROM_BROKER,
RETRY_IN_MEMORY_TASK
}
```

