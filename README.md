# Telco Commons Kafka Java Library

![Build](https://github.com/inomera/telco-commons-kafka/workflows/Build/badge.svg)


Telco Commons Kafka is a lightweight, Spring-friendly Kafka integration library designed for enterprise-scale messaging systems. It provides seamless support for multiple serialization formats including JSON, Kryo, Protobuf, and Smile, Avro(next minor version) and offers ready-to-use configuration properties for both producers and consumers. With a focus on simplicity, type-safety, and flexibility, this library makes it easy to build reactive and event-driven microservices using Apache Kafka and Spring Boot.

Use lightweigth Virtual Threads.. minimum resources, maximum capacity of process messages!!

# Version Compatability

Compatability Matrix

| Version | JDK   | Spring  | Kafka Client | Kafka Server |
|---------|-------|---------|--------------|--------------|
| v4.x.x  | 23 | 3.4.2   | 4.0.0 >=     | 4.0.0 >=     |
| v3.x.x  | 17 | 3.1.5   | 3.0.2 >=     | 3.0.0 >=     |
| v2.4.x  | 17 | 3.1.5   | 1.1.0 >=     | 1.1.0 >=     |
| v2.3.x  | 8,11 | 2.7.12+ | 3.0.2 >=     | 3.0.0 >=     |

## Subprojects

| Artifact       | Version                                                                                                                                                                                                                          |
|----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| spring-kafka   | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/spring-kafka/badge.svg?version=4.0.1)](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/spring-kafka)     |
| kafka-json     | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/kafka-json/badge.svg?version=4.0.1)](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/kafka-json)         |
| kafka-kryo     | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/kafka-kryo/badge.svg?version=4.0.1)](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/kafka-kryo)         |
| kafka-protobuf | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/kafka-protobuf/badge.svg?version=4.0.1)](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/kafka-protobuf) |
| kafka-smile    | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/kafka-smile/badge.svg?version=4.0.1)](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/kafka-smile)       |
| -              | -                                                                                                                                                                                                                                |
| spring-kafka   | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/spring-kafka/badge.svg?version=3.0.7)](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/spring-kafka)     |
| kafka-json     | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/kafka-json/badge.svg?version=3.0.7)](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/kafka-json)         |
| kafka-kryo     | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/kafka-kryo/badge.svg?version=3.0.7)](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/kafka-kryo)         |
| kafka-protobuf | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/kafka-protobuf/badge.svg?version=3.0.7)](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/kafka-protobuf) |
| kafka-smile    | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/kafka-smile/badge.svg?version=3.0.7)](https://maven-badges.herokuapp.com/maven-central/com.inomera.telco.commons/kafka-smile)       |



## With Gradle

JDK 23 Support

```groovy
implementation 'com.inomera.telco.commons:spring-kafka:4.0.1'
implementation 'com.inomera.telco.commons:kafka-json:4.0.1'
implementation 'com.inomera.telco.commons:kafka-kryo:4.0.1'
implementation 'com.inomera.telco.commons:kafka-protobuf:4.0.1'
implementation 'com.inomera.telco.commons:kafka-smile:4.0.1'
```

JDK 17 Support

```groovy
implementation 'com.inomera.telco.commons:spring-kafka:3.0.7'
implementation 'com.inomera.telco.commons:kafka-json:3.0.7'
implementation 'com.inomera.telco.commons:kafka-kryo:3.0.7'
implementation 'com.inomera.telco.commons:kafka-protobuf:3.0.7'
implementation 'com.inomera.telco.commons:kafka-smile:3.0.7'
```

```groovy
implementation 'com.inomera.telco.commons:spring-kafka:2.4.0'
implementation 'com.inomera.telco.commons:kafka-json:2.4.0'
implementation 'com.inomera.telco.commons:kafka-kryo:2.4.0'
implementation 'com.inomera.telco.commons:kafka-protobuf:2.4.0'
implementation 'com.inomera.telco.commons:kafka-smile:2.4.0'
```

JDK 8 & 11 Support (!!Not published to central maven repository!!)

```groovy
implementation 'com.inomera.telco.commons:spring-kafka:2.3.8'
implementation 'com.inomera.telco.commons:kafka-json:2.3.8'
implementation 'com.inomera.telco.commons:kafka-kryo:2.3.8'
implementation 'com.inomera.telco.commons:kafka-protobuf:2.3.8'
implementation 'com.inomera.telco.commons:kafka-smile:2.3.8'
```

Try to use new versions.

### Important Notes

- Library support legacy OS threads and also virtual threads!!!! (4.x.x)
- Support from JDK23 and 4.X.X version
- Support from JDK17 and JDK17+
- Support Spring Boot 3.X.X version

- Use 3.X.X version of spring-kafka library if you are use JDK17 and above version and Spring Boot 3.X.X
- Use 2.X.X version of kafka-kryo library if you are use JDK17 and above version and Spring Boot 3.X.X
- Use 2.X.X version of kafka-protobuf library if you are use JDK17 and above version and Spring Boot 3.X.X
- Use 2.X.X version of kafka-smile library if you are use JDK17 and above version and Spring Boot 3.X.X


# Benchmark (OS vs Virtual Threads)

---

## 🧪 **Benchmark Setup**

- **Kafka Cluster**: 3 nodes
- **Producer Messages**: 10K and 100K
- **JVM Args**: `-Xms50m -Xmx2256m`
- **Instance**: Single JVM
- **max.poll.records**: 1000
- **Worker threads (for OS threads case)**: 360
- **Worker threads delay**: 100 ms(millis)
- **Lib version**: 4.0.1

---

## 📊 10K Records - Virtual Threads

| Case | Consumer Configs             | Duration    | TPS     | Max CPU | Memory |
|------|------------------------------|-------------|---------|---------|--------|
| 1    | AT_MOST_ONCE_BULK           | ~0.978s     | 10K     | 13%     | 104 MB |
| 2    | AT_MOST_ONCE_SINGLE         | ~118s       | 85      | 13%     | 104 MB |
| 3    | AT_LEAST_ONCE_BULK          | ~1.052s     | 10K     | 13%     | 104 MB |
| 4    | AT_LEAST_ONCE_SINGLE        | ~1.152s     | 10K     | 13%     | 104 MB |

---

## 📊 10K Records - OS Threads

| Case | Consumer Configs             | Duration    | TPS     | Max CPU | Memory | Worker Threads |
|------|------------------------------|-------------|---------|---------|--------|----------------|
| 1    | AT_MOST_ONCE_BULK           | ~3.019s     | 3.33K   | 13%     | 77 MB  | 360            |
| 2    | AT_MOST_ONCE_SINGLE         | ~118s       | 85      | 13%     | 77 MB  | 360            |
| 3    | AT_LEAST_ONCE_BULK          | ~3.067s     | 3.33K   | 13%     | 77 MB  | 360            |
| 4    | AT_LEAST_ONCE_SINGLE        | ~3.151s     | 3.1K    | 13%     | 77 MB  | 360            |

---

## 📊 100K Records - Virtual Threads

| Case | Consumer Configs             | Duration    | TPS     | Max CPU | Memory |
|------|------------------------------|-------------|---------|---------|--------|
| 1    | AT_MOST_ONCE_BULK           | ~2.595s     | 38.4K   | 24%     | 310 MB |

---

## 📊 100K Records - OS Threads

| Case | Consumer Configs             | Duration    | TPS     | Max CPU | Memory | Worker Threads |
|------|------------------------------|-------------|---------|---------|--------|----------------|
| 1    | AT_MOST_ONCE_BULK           | ~28.794s    | 3.4K    | 26%     | 290 MB | 360            |

---

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

### Virtual Thread based Consumer

**Support all of consumer types topic based, bulk, partition key based, ordered and unordered message processing**

```java
@Bean
public KafkaMessageConsumer virtualConsumer(VirtualKafkaConsumerBuilder virtualKafkaConsumerBuilder,
					KafkaConsumerConfigurationProperties defaultKafkaConsumerConfigurationProperties,
					KafkaProtobufDeserializer kafkaDeserializer) {

Properties consumerProperties = defaultKafkaConsumerConfigurationProperties.getProperties();
int partitionNumber = (int) consumerProperties.getOrDefault("partition.number", 6);
return virtualKafkaConsumerBuilder.properties(consumerProperties)
	.groupId(VIRTUAL_EVENT_LOGGER)
	.topics(KafkaTopicUtils.getTopicNames(
		PlayerCreateCommandProto.class,
		OrderMessage.class,
		PaymentMessage.class
	))
	.offsetCommitStrategy(defaultKafkaConsumerConfigurationProperties.getOffsetCommitStrategy())
	.valueDeserializer(kafkaDeserializer)
	.autoPartitionPause(true)
	.invoker()
	.unordered()
	.custom(new CustomPartitionKeyAwareVirtualExecutorStrategy(partitionNumber, VIRTUAL_EVENT_LOGGER))
	.and()
	.threadStore(consumerThreadStore())
	.build();
}
```


### KafkaListener

Enable spring kafka configration via @EnableKafkaListeners annotation

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


@KafkaListener annotation capabilities

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
RETRY_FROM_BROKER : do not ack/commit message to broker! re-start consumer, consumer polls message from broker again.
RETRY_IN_MEMORY_TASK : commit/ack message to broker, retry in consumer local queue. default retryCount val is 3.
 */
enum RETRY {
NONE,
RETRY_FROM_BROKER,
RETRY_IN_MEMORY_TASK
}
```

Custom partition key handling for ordering messages (binary message format)

```java
static class CustomPartitionKeyAwareVirtualExecutorStrategy extends PartitionKeyAwareVirtualExecutorStrategy {

        public CustomPartitionKeyAwareVirtualExecutorStrategy(int partitionPoolSize, String groupId) {
            super(partitionPoolSize, new IncrementalNamingVirtualThreadFactory(String.format(INVOKER_THREAD_NAME_FORMAT, groupId)));
        }

        @Override
        protected int getPartitionKey(ConsumerRecord<String, ?> record) {
            if (record.value() instanceof GeneratedMessage message) {
                PartitionMessage partition = ProtobufUtils.getField(message, "partition", PartitionMessage.class);
                if (partition != null) {
                    return partition.getPartitionKey().hashCode();
                }
                return super.getPartitionKey(record);
            }
            return super.getPartitionKey(record);
        }

    }
   ```
 
## Publishing

To publish a version to maven repository,
you should create a gradle.properties file in the root directory of this project.

The file is: `/path-to-project/gradle.properties`

This file is included in .gitignore file.
You should not commit it since it contains sensitive information.

Add credentials for maven repository to `gradle.properties` file.

Example `gradle.properties` file:

```
mavenReleaseUrl=https://oss.sonatype.org/service/local/staging/deploy/maven2/
mavenSnapshotUrl=https://oss.sonatype.org/content/repositories/snapshots/
mavenUsername=************************
mavenPassword=************************
mavenPackageGroup=com.inomera

signing.keyId=******
signing.password=******
signing.secretKeyRingFile=******.gpg
```

Then you need to invoke `release.sh` script in the project root directory.

```sh
# When the latest VERSION is 1.1.1

./release.sh --release-type patch --project $projectName
# New version is 1.1.2

./release.sh --release-type minor --project $projectName
# New version is 1.2.0

./release.sh --release-type major --project $projectName
# New version is 2.0.0
```

To publish a snapshot release, use `--snapshot` flag as follows:

```sh
./release.sh --release-type latest --project $projectName --snapshot
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
