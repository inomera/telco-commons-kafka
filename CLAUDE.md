# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Version lines — confirm the branch first

Several release lines are maintained in parallel and the JDK / Spring / `kafka-clients` versions differ per line. **Check `git branch --show-current` before editing**, and expect a fix to need backporting.

| Branch | `spring-kafka` | Java | Spring | kafka-clients | Gradle |
|---|---|---|---|---|---|
| `v4.x.x` (current) | **4.1.3** | **25** | **7.0.8** (Boot 4.0.7) | **4.1.2** | **9.2.1** |
| `master` | 4.0.0 | 23 | 6.2.1 | 4.0.0 | 8.12.1 |
| `v3.x.x` | 3.0.7 | 17 | 6.0.13 | 3.0.2 | — |
| `v2.3.x` | 2.3.8 | 8/11 | 5.3.x | 3.0.2 | 7.4 |

All shared dependency versions — including Spring, Jackson, protobuf, Kryo and the JDK
level — live in the root `build.gradle` `versions` map. The Java toolchain and compiler
options are configured once in the root `subprojects` block, not per module.

`kafka_clients` and the other shared versions live in the **root `build.gradle` `versions` map**, not per module. README.md carries the user-facing compatibility matrix (also lists Kafka *server* ≥ 4.0.0 for this line).

Each publishable module has its own `VERSION` file and versions independently — `kafka/spring-kafka` is 4.1.3 while the serializer modules sit at 4.1.0, because only the library has changed since. Bump the patch version of the modules a change actually touches; do not move them in lockstep. Git tags are `<module>/<version>` (e.g. `spring-kafka/4.1.3`).

## Build & test

**JDK 25 is required.** CI uses temurin 25 (`.github/workflows/gradle.yml`, on push to `master` / `v4.x.x`). The build resolves a Java 25 toolchain, so a JDK 25 must be discoverable; the simplest way is to run Gradle on it.

```bash
./gradlew build                     # everything (this is what CI runs)
./gradlew :spring-kafka:build
./gradlew :spring-kafka:test
./gradlew :kafka-json:test --tests '*JsonKafkaSerializerTest*'
./gradlew build -PwithCoverage      # coverage is opt-in, see below
./gradlew :kafka-json:jacocoTestReport -PwithCoverage
```

`PERFORMANCE.md` holds the measured throughput/allocation/GC report and how to reproduce it
(`ThroughputBenchmarkTest`, opt-in via `-Pbenchmark=true`, collector sweeps via
`-PbenchmarkJvmArgs`). Read its caveats before quoting any number from it.

Coverage is behind `-PwithCoverage`: the JaCoCo agent attaches to every `Test` task as soon as
the plugin is applied, which would make it a hard dependency of a plain `./gradlew build`. The
tool version is 0.8.14, the first release that can instrument Java 25 class files.

Only the five `kafka/*` modules matter for a library change. The two `example/` apps pull in the
Spring Boot and protobuf Gradle plugins, so `--configure-on-demand` plus explicit module paths
is the quickest way to build just the library:

```bash
./gradlew --configure-on-demand :spring-kafka:build :kafka-json:build
```

Gradle paths do not mirror directory names (see `settings.gradle`):
`:spring-kafka` → `kafka/spring-kafka`, `:kafka-json` → `kafka/json`, `:kafka-kryo` → `kafka/kryo`, `:kafka-smile` → `kafka/smile`, `:kafka-protobuf` → `kafka/protobuf`, `:spring-kafka-kryo` / `:spring-kafka-protobuf` → `example/…`.

`settings.gradle` includes the two `example/` Spring Boot apps, so every Gradle invocation resolves the Spring Boot plugin even when building an unrelated module.

Test coverage of the consumer runtime is thin but no longer absent:
`ConsumerPollerTest` drives the poll loop through Kafka's `MockConsumer` (commit ordering,
incremental rebalance, cross-thread `pause`), and `RetryBehaviourTest` covers backoff, the
capacity ceiling and dead-lettering. Extend those rather than testing by hand.

`KafkaE2eTest` runs against a **real Kafka 4 cluster** and covers the failure modes 4.1.0 fixes:
bulk batch offsets, KIP-848 cooperative rebalance, `RETRY_FROM_BROKER` not killing the poller,
in-memory retry backoff, an undecodable payload, the bounded virtual dispatcher's concurrency cap,
no message loss across an abrupt restart, and failover to a surviving group member. Start the
broker first:

```bash
cd example/spring-kafka-protobuf/src/main/resources && docker compose up -d
```

That compose file is a 3-broker KRaft cluster with `group.protocol=consumer` enabled, exposed on
`127.0.0.1:10000,20000,30000`. When nothing answers there the whole class skips itself, so
`./gradlew build` still passes without Docker — check for skipped tests rather than assuming the
E2E scenarios ran.

Two harness details worth knowing before adding poll-loop tests: schedule work with
`MockConsumer.schedulePollTask` **before** `poller.start()`, and observe results through
lock-free fields rather than calling `MockConsumer` from the test thread. Every `MockConsumer`
method is `synchronized` and an idle poll loop hammers that monitor, so inspecting the consumer
directly measures lock fairness instead of poller behaviour (see `RecordingConsumer` in
`ConsumerPollerTest`).

## Publishing

Artifacts go to **Maven Central** with GPG signing. Credentials and signing keys come from a root `gradle.properties`, which is **gitignored** — never commit it. Expected keys: `mavenUsername`, `mavenPassword`, `mavenReleaseUrl`, `mavenSnapshotUrl`, `mavenPackageGroup`, `signing.keyId`, `signing.password`, `signing.secretKeyRingFile`.

`SNAPSHOT_RELEASE=true` switches the target repo to the snapshot URL and skips signing.

All of it lives in `gradle/publishing.gradle`; each module only declares `pubName`,
`pubDescription` and `pubRepo` in its `ext` block before applying the script. Sources and javadoc
jars come from `java { withSourcesJar(); withJavadocJar() }` and `signing.sign(publication)`
covers the POM and every artifact, so there is no manual `.asc` wiring. Do not reintroduce the
`archives` configuration, a `signArchives` task, or a `model {}` block — all three were removed in
Gradle 9.

Releases run through the **per-module** script, from inside that module's directory:

```bash
cd kafka/spring-kafka && ./release.sh [patch|minor|major] [--force-new-version]
```

It bumps `VERSION` via `docker run treeder/bump`, runs `../../gradlew clean build`, commits `Release version <module>/<version>`, tags, pushes, then `../../gradlew :<module>:publish`. Requires Docker and push rights — do not run it unless asked. Note README documents a root `./release.sh --release-type X --project Y` that does not exist on this branch.

The old per-module publish blocks printed the Maven Central token to stdout with
`println("password -> …")`. That is gone; do not add anything that logs credentials.

## Architecture

An alternative to Spring's own `spring-kafka`: a hand-rolled consumer runtime plus interchangeable serializers. Two independent halves.

### Serialization modules (`kafka/json`, `kryo`, `smile`, `protobuf`)

Each ships a `Serializer`/`Deserializer` pair sharing one wire format: **a 4-byte int class id followed by the payload**. A `ClassIdRegistry` (usually `ImmutableClassIdRegistry`) maps id ↔ `Class`, so the consumer resolves the concrete type without type headers. Adding a message type means registering the same id on **both** producer and consumer; unregistered ids/classes throw.

Deserializers **return `null` instead of throwing** — a throwing `Deserializer` kills the Kafka consumer thread. The poller treats `null` as "log and continue", so a malformed message is silently skipped and its offset committed. There is no dead-letter hook anywhere in the library.

### Consumer runtime (`kafka/spring-kafka`)

Registration:

```
@EnableKafkaListeners
  → KafkaListenerConfigurationSelector → KafkaImportBeanDefinitionRegistrar
      → registers ListenerMethodRegistry + KafkaListenerAnnotationBeanPostProcessor
  → BPP scans beans for @KafkaListener methods (must have exactly one parameter)
      → ListenerMethodRegistry.addListenerMethod(groupId[, topic], messageType, ListenerMethod)
```

`KafkaBootstrapConfiguration` exposes **two prototype-scoped builders**: `KafkaConsumerBuilder` and `VirtualKafkaConsumerBuilder`. Inject whichever matches the threading model you want.

Dispatch is keyed on `(groupId, topic, messageType)` through `ListenerEndpointDescriptor` subclasses — topic-scoped vs all-topics × exact type vs `isAssignableFrom` (`includeSubclasses = true`). **The `groupId` on `@KafkaListener` must match the `groupId` on the builder**; otherwise the record falls through to `ListenerMethodNotFoundHandler` and is dropped.

Runtime:

```
DefaultConsumerPoller  (one OS thread; also the ConsumerRebalanceListener)
  → ConsumerInvoker
      → ExecutorStrategy.get(record)            → ThreadPoolExecutor      (OS-thread path)
        VirtualExecutorStrategy.getExecutor(rec) → ExecutorService         (virtual path)
          → MethodInvoker.addRecord() → FutureTask<InvokerResult> → ListenerMethod (reflection)
  → poller tracks a Future per TopicPartition, commits offsets, pauses/resumes partitions
```

`buildBulk()` swaps in the parallel `Bulk*` chain (`BulkConsumerPoller`, `BulkConsumerInvoker`, `BulkInvokerResult`), where a listener takes a `Set<Message>` per partition instead of a single message.

### The two threading stacks

This is the defining feature of the 4.x line, and the main thing to get right when editing:

| | OS threads | Virtual threads |
|---|---|---|
| Builder | `KafkaConsumerBuilder` | `builder.virtual.VirtualKafkaConsumerBuilder` |
| Invoker builder | `ConsumerInvokerBuilder` | `builder.virtual.VirtualConsumerInvokerBuilder` |
| Strategy SPI | `consumer.executor.ExecutorStrategy` → `ThreadPoolExecutor` | `consumer.executor.virtual.VirtualExecutorStrategy` → `ExecutorService` |
| Invokers | `SimpleConsumerInvoker`, `RejectionAwareConsumerInvoker`, `Bulk*` | `consumer.invoker.virtual.Virtual*` |
| Intended for | CPU-bound listeners | IO-bound listeners (DB, HTTP, messaging) |

The two stacks are **near-duplicates** — a change to `RejectionAwareConsumerInvoker`, an executor strategy, or a builder almost always needs the mirror change under the corresponding `virtual/` package. They share `MethodInvoker`, `ListenerMethodRegistry`, `RejectionHandler`, both pollers, and the whole retry package.

Both stacks use the **same pollers**, and the poller thread must stay an **OS (platform) thread** — this is deliberate and called out in `VirtualKafkaConsumerBuilder.getOrCreateConsumerThreadFactory()` ("The thread should be OS Thread!! Because it is kafka consumer thread factory!!!"). Only listener execution moves onto virtual threads.

`VirtualExecutorStrategy.getExecutor()` is a default method that dispatches with `instanceof` over exactly two sub-interfaces — `ParameterBasedVirtualExecutorStrategy` (per-record `get(record)`) and `DefaultVirtualExecutorStrategy` (single `get()`). A custom strategy that implements neither will `ClassCastException` at runtime; implement one of the two.

`README.md` has an OS-vs-virtual benchmark section with the project's own measured numbers — useful context before changing anything on the dispatch path.

### Key behaviours and gotchas

- **`KafkaConsumer` is only thread-safe for `wakeup()`.** Everything else (`poll`, `pause`, `resume`, `commitSync`, `seek`) must run on the poller thread. `ConsumerPoller.pause(TopicPartition)` is callable from any thread because it enqueues a command onto `AbstractConsumerPoller`'s command queue and wakes the loop; the loop drains commands at the top of each iteration. **Never add a code path that touches the consumer directly from another thread** — add a command instead.
- Both pollers extend `AbstractConsumerPoller<R>`, which owns the lifecycle, the command queue, offset accounting and rebalance handling. Subclasses only implement `dispatch`, `highestOffset`, `onResultCompleted` and `assertReadyToStart`. `BulkConsumerPoller` used to subclass `DefaultConsumerPoller` while shadowing seven of its fields, which is why fixes kept applying to only one of them.
- `onResultCompleted` returns a boolean: `false` withholds that offset **and everything after it on the same partition**. This is how retry-from-broker and a refused retry avoid losing records.
- Offsets: at-most-once still commits synchronously before dispatch, because the guarantee depends on it. At-least-once accumulates offsets and flushes one `commitAsync` per poll cycle. `AT_MOST_ONCE_SINGLE` remains a synchronous round-trip per record — measured at ~116 TPS versus ~10K for the `_BULK` variants (`PERFORMANCE.md`), so prefer `_BULK` unless per-record commit is a hard requirement.
- **Broker-side rebalancing (KIP-848)** is opt-in via `builder.serverSideRebalance([assignor])`, which sets `group.protocol=consumer` and drops the three settings the client rejects alongside it — `session.timeout.ms`, `heartbeat.interval.ms`, `partition.assignment.strategy` — with a warning, because those keys are common in existing property files and otherwise fail consumer construction with a `ConfigException`. Under that protocol assignment is always incremental, which is exactly what the `onPartitionsAssigned` / `onPartitionsLost` handling in `AbstractConsumerPoller` is written for. Pattern subscriptions work: the broker resolves the regex. See `GroupProtocol`.
- `autoPartitionPause(true)` is the coarse backpressure: a partition stays paused while its records are in flight. On the virtual path a thread-per-task executor never rejects, so that was historically the *only* brake. Use `.boundedExecutor(maxConcurrency)` (or `BoundedVirtualExecutorStrategy`) to cap concurrent invocations; exceeding the cap surfaces as `RejectedExecutionException` and routes through the normal rejection path.
- **Failure signalling is implicit.** `ListenerMethod.invoke` returns the `@KafkaListener` annotation *only when the listener threw* (`InvocationTargetException`) and `null` on success — so `InvokerResult.getKafkaListener() != null` means "this record failed", and that is what drives the retry decision in `DefaultRecordRetryer`. All other exceptions are logged and swallowed.
- `MethodInvoker` resolves listeners via `ListenerMethodRegistry.resolveListenerMethods`, which memoizes `(groupId, topic, messageType)` and is invalidated on registration. It dispatches with an explicit loop; do not go back to `peek()` + `count()`, which only invoked listeners by accident of a `filter` sitting in front of the terminal op.
- Bulk listeners take a `Set`, so payloads that are `equals()` still collapse. The batch is collected into a `LinkedHashSet` to at least preserve offset order, and a collapse is logged at `WARN` rather than passing silently.
- `ListenerInvocationInterceptor` takes `(Object message, Headers headers)` — both methods are `default`, so implementations override only what they need.

### Retry

`@KafkaListener(retry = …, retryCount = …, retryBackoffTime = …)`. Retry only applies to the `AT_LEAST_ONCE_*` commit strategies. The annotation's own comments mark the feature "experimental level".

- `NONE` — no retry.
- `RETRY_FROM_BROKER` — the offset is withheld, so the broker re-delivers the record. It no longer throws out of the poll loop, which used to kill the poller thread and, after rebalancing, could take the whole consumer group down on one poison record.
- `RETRY_IN_MEMORY_TASK` — the record is scheduled on the consumer's own `DelayQueue` and re-dispatched through the invoker after a real backoff.

Design points to preserve when touching `consumer/retry`:

- Queues are **instance** fields. They used to be `public static`, so all consumers in a JVM shared one queue and a retry could execute against the wrong `groupId`, silently dropping the record.
- `RetryPolicy` computes exponential backoff with 50–100% jitter, and `RetryContext` implements `Delayed` so the worker parks until an attempt is due. Re-queueing without a delay turns a permanently failing record into a hot retry loop against the downstream.
- Capacity is a hard `Semaphore` ceiling. `offer` returns `false` when full and the caller **withholds the offset** instead of dropping the record.
- `future.get` uses `RetryPolicy.invocationTimeout`; a hung listener must not wedge the single retry thread.
- Exhausted records go to a `DeadLetterHandler` (`LoggingDeadLetterHandler` by default, which logs coordinates only, never payloads).
- In-memory retry retains the deserialized payload for the lifetime of the retry, so `capacity` is the memory bound. Broker-backed retry (pause + `seek`) would remove that retention; it is not implemented yet.

### Liveness / self-healing

Opt-in. `PollerThreadStore` (a `ConsumerThreadStore`) records the poller thread; `PollerThreadStateChecker` samples thread state on a schedule (`poller.thread.checker.active` / `.initialDelaySec` / `.retryAsSec`, passed in as `Properties`) and calls `ConsumerPoller.start()` again when `shouldRestart()` is true — i.e. `running && closed`, meaning the loop exited while still asked to run. Without `threadStore(...)` on the builder, a dead poller is never noticed.

Liveness is read from the stored `Thread` reference (`PollerThreadState.getThread().isAlive()`). It used to scan `Thread.getAllStackTraces()` once per monitored thread, which dumps every stack in the JVM on a timer and never reports virtual threads at all.

### Producers

- `KafkaMessagePublisher` — non-transactional; asserts `transactional.id` is *absent*. Returns a `CompletableFuture<SendResult>`. When the payload is not `PartitionKeyAware` it derives the partition key from `data.hashCode()`, which is not stable across JVMs for types that do not override `hashCode`.
- `KafkaTransactionalMessagePublisher` — asserts `transactional.id` is *present*, and wraps **each individual `send`** in its own transaction (`createProducer` → `beginTransaction` → `send` → `commitTransaction` → `close`) over `DefaultKafkaProducerFactory` / `CloseSafeProducer`. `beginTransaction` failures propagate as a thrown exception while other failures come back as a failed future. Its header support serializes values with `org.springframework.util.SerializationUtils` (Java serialization) — do not extend that path; encode headers by type instead.

## Conventions

- Indentation is inconsistent (some files use tabs, most 4 spaces) and there is no `.editorconfig`. Match the file you are editing.
- Lombok throughout (`@Getter`, `@RequiredArgsConstructor`, `@Slf4j`); `lombok.config` at the root.
- Two `Assert` classes are both in use — `com.inomera.telco.commons.lang.Assert` and `org.springframework.util.Assert`. Follow the surrounding file.
- Modules expose Spring/Kafka as `api` (not `implementation`) so consumers inherit them transitively; keep new public-API dependencies on `api`.
- Nullability annotations come from **JSpecify** (`org.jspecify.annotations.Nullable`). Spring Framework 7 deprecated `org.springframework.lang.Nullable`; do not reintroduce it. JSpecify's `@Nullable` is `TYPE_USE`, so it annotates the type, not the declaration.
- `-Xlint:deprecation` is enabled for `compileJava` (main sources only — the checked-in generated protobuf test fixtures would otherwise flood it). Main sources currently compile warning-free; keep them that way.
- **Never log `record.value()`.** Payloads used to be logged at `WARN`/`ERROR` throughout the consumer path; log `topic-partition-offset` instead. A poison-message storm otherwise turns synchronous logging into the outage.
- `InterruptUtils.interruptIfInterruptedException(e)` is the idiom for restoring the interrupt flag after a broad `catch (Exception e)`.
- `HostUtils.getHostname()` resolves once at class init from `$HOSTNAME`, then `InetAddress`, then the pid. Do not reintroduce the `Runtime.exec("hostname")` fork.
- Use `Math.floorMod`, never `Math.abs(x) % n`, when mapping a hash onto a bucket: `Math.abs(Integer.MIN_VALUE)` is negative.
- **Never hand a `ThreadFactory` straight to an `Executors.*` factory.** `newThread` must return an *unstarted* thread, and telco-commons-lang's `IncrementalNamingVirtualThreadFactory` builds its thread with `Thread.ofVirtual().name(..).start(runnable)` — already running. An executor then calls `start()` again and every task dies with `IllegalThreadStateException`, which surfaces as one "Error dispatching..." per record while the work is silently dropped. Its `IncrementalNamingUnstartedVirtualThreadFactory` sibling is the one that works. Route caller-supplied factories through `VirtualThreadFactories.unstartedVirtual(...)`, which probes the contract and falls back with a warning.
- Executor shutdown should `shutdown()` then `awaitTermination(...)` with a bounded timeout, and restore the interrupt flag on `InterruptedException`. Do not use try-with-resources on an `ExecutorService` while holding a lock — `close()` blocks until termination.
