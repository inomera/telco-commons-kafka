# Release notes

## spring-kafka 4.1.5

Eight consumer-runtime defects, found while reviewing the 4.0.1 → 4.1.3 delta and while running the
failure-mode suite against a live 3-broker KRaft cluster under **both** group protocols. Three of
them are only reachable under `group.protocol=consumer` (KIP-848) and were invisible until the
suite was run a second time with the protocol flipped.

No API was removed. `AbstractConsumerPoller` gains one abstract method — see *Upgrading*.

### Fixed

**1. A withheld offset was only withheld for one poll cycle.** `reapCompleted()` stopped at the
first refused verdict, but the refused future had already been removed from the queue, so the next
cycle started at the record *after* it, found it committable and committed straight over the top.
That is silent loss of exactly the record `RETRY_FROM_BROKER` exists to protect. Partitions are now
pinned until every withheld offset on them resolves; a pinned partition keeps consuming and only
its committed offset freezes, so the symptom is non-decreasing lag rather than a stall. Offsets
strictly below the earliest pin still commit.

**2. A rebalance committed work it never checked.** `onPartitionsRevoked` walked the in-flight queue
and committed every finished future without asking whether the listener had succeeded, so a
handover landing on a failure committed over it. It now uses a side-effect-free `isCommittable()`
and skips pinned partitions. Running the full retry hook there is not an option — it would schedule
a retry for a partition being handed away.

**3. `serverSideRebalance()` was order-sensitive.** It applied immediately, and `properties(props)`
replaces the whole `Properties` instance, so `.serverSideRebalance().properties(props)` silently
dropped `group.protocol` and the consumer came up on the classic protocol with no indication that
anything was wrong. The request is now recorded and applied at build time.

**4. `RETRY_IN_MEMORY_TASK` broke at-least-once.** Accepting a record for retry committed its offset
straight away, telling the broker the record was consumed while the only copy of it lived in this
JVM's heap — a restart, an OOM or a rolling deploy lost it. The offset is now held until the new
`RetryOutcomeListener` reports the attempt terminal, on success **or** dead-letter. A custom
`InMemoryRecordRetryConsumer` that does not override `reportsRetryOutcome()` keeps the old
commit-on-accept behaviour, because withholding with nothing to release the pin would freeze the
partition's offset for good.

**5. `WakeupException` escaped the synchronous commit paths.** A `pause()` issued from another
thread interrupts any blocking consumer call, not just `poll()`, and only `CommitFailedException`
was caught. The commit is retried once instead — the flag is consumed by the throw — and skipped
while stopping so shutdown is not delayed.

**6. An at-most-once pre-dispatch commit could kill the consumer or drop a batch.** Both commit
helpers caught only `CommitFailedException`, so a `TimeoutException` from an unreachable
coordinator unwound the poll loop and killed the poller. A *refused* commit was no better: `poll()`
had already moved the fetch position past the batch, so abandoning the cycle threw those records
away for good — on the one strategy whose users chose it because they cannot reprocess a record.
Both helpers now catch everything, and the loop rewinds to the first unhandled record so the commit
is retried without ever redelivering one.

> The usual refusal under `group.protocol=consumer` is `STALE_MEMBER_EPOCH`, which the client does
> **not** retry for you: `StaleMemberEpochException` is an `ApiException`, not a
> `RetriableException`, so `commitSync` gives up on the first response even though the error text
> asks the caller to retry.

**7. `group.protocol=consumer` set in `application.yml` was never normalised.** The clean-up only
ran when `serverSideRebalance()` was called in code. Configured the usual way — one line in the
same `properties` block as `session.timeout.ms` — the setting reached the client untouched and the
consumer died at construction with `ConfigException: heartbeat.interval.ms, session.timeout.ms
cannot be set when group.protocol=CONSUMER`. Normalisation moved to
`AbstractConsumerPoller.buildConsumerProperties()`, the single point every entry point funnels
through, and it no longer mutates the caller's `Properties`.

**8. `poll()` throwing killed the consumer.** Under the classic protocol `poll()` just returns empty
while the cluster is unreachable; under `group.protocol=consumer` it surfaces the failure as a
`TimeoutException` once it cannot reach the coordinator within `default.api.timeout.ms`. That
unwound the loop, closed the consumer and left the pod running with nothing consuming. Recovery is
the poll loop's job, so it now stays in the loop and retries with escalating backoff.

### Changed behaviour to be aware of

- **`RETRY_IN_MEMORY_TASK`** holds the partition's committed offset for the duration of the retry.
  Lag on that partition rises during the retry window; in exchange the record is not lost if the
  process dies. (Defect 4.)
- **`RETRY_FROM_BROKER`** pins the partition permanently until a restart or rebalance. Consumption
  continues, but committed offsets stop advancing on that partition. Previously the record was
  quietly committed over. (Defect 1.)
- **Log level**: a refused pre-dispatch commit is now `WARN` with a single line, not `INFO` with a
  stack trace. It discards nothing any more, but it is worth seeing.

### Upgrading

`AbstractConsumerPoller` has a new abstract method:

```java
protected abstract boolean isCommittable(R result);
```

Both in-tree pollers implement it. Anything subclassing `AbstractConsumerPoller` directly must add
it — deliberately abstract rather than defaulted, so the compiler catches it instead of a default
silently reintroducing defect 2.

Requires **JDK 25** and Spring Framework 7 / Spring Boot 4, unchanged from 4.1.x.

### Verification

Run against a live 3-broker KRaft cluster (`bitnami/kafka:4.0.0`) on JDK 25.0.2, Spring Boot 4.0.7,
Spring 7.0.8, `kafka-clients` 4.1.2, Gradle 9.2.1.

| | classic | consumer (KIP-848) |
|---|---|---|
| Executed | 42 | 42 |
| Passed | **42** | **42** |
| Failed | 0 | 0 |
| Skipped (opt-in benchmarks) | 6 | 6 |

New `KafkaChaosE2eTest` covers broker and full-cluster outages, commit refusals and timeouts, a
rebalance against a pinned partition, poller self-heal, a listener blocking past
`max.poll.interval.ms`, and the `KIP-848 + AT_MOST_ONCE_BULK` shape running in production.

Two defects were confirmed by first running the new test against the old behaviour: defect 1
reproduced as `committed offset 4 moved past the withheld record at offset 1`, and defect 6 as a
poll loop that died on the first commit throw and never attempted a second. Defects 7 and 8 were
surfaced by the first `group.protocol=consumer` run.

The protocol is selected in `kafka/spring-kafka/src/test/resources/e2e-kafka.yml`, shaped like a
real `application.yml` properties block and deliberately keeping the classic-only settings in place
so the consumer-protocol run proves they are stripped rather than fatal.
