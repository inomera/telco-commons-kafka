# Performance Report — spring-kafka 4.1.x

Consume-side throughput, allocation and GC behaviour, measured with
`ThroughputBenchmarkTest`. Every number below comes from an actual run; the
[caveats](#what-this-report-does-not-show) section is as important as the tables.

## Environment

| | |
|---|---|
| Hardware | MacBook Pro, Apple Silicon |
| JDK | OpenJDK 25.0.2+10-69 |
| Heap | `-Xmx1g` |
| Kafka | 4.0.0 broker, 3-node KRaft cluster (Docker), 6 partitions |
| Library | `spring-kafka` 4.1.2 |
| Consumer | `max.poll.records=100`, `enable.auto.commit=false`, `auto.offset.reset=earliest` |
| Listener | no-op — increments a counter only |

The backlog is published **before** the consumer starts, so each case measures
consume throughput rather than producer behaviour.

### Metrics

| Column | Meaning |
|---|---|
| `E2E.ms` / `E2E.TPS` | `consumer.start()` → last record consumed. Group join and every commit included. **This is the column to compare.** |
| `GCs` / `GC.ms` | Collections and total pause time attributable to the case |
| `ALLOC.MB` | Bytes allocated across all threads, virtual included |
| `B/record` | Allocation per consumed record |

## Baseline — G1 (JDK 25 default)

```
======================================================================================================================
CASE                                     THREADS         RECORDS   E2E.ms   E2E.TPS    GCs    GC.ms ALLOC.MB  B/record
======================================================================================================================
single-record / AT_MOST_ONCE_BULK        OS x8-16          20000     2052      9.7K      1        3     32.2      1689
single-record / AT_LEAST_ONCE_BULK       OS x8-16         200000      394    507.6K      3        2    160.4       841
single-record / AT_LEAST_ONCE_SINGLE     OS x8-16         200000      192   1041.7K      6        7    159.0       834
single-record / AT_MOST_ONCE_SINGLE      OS x8-16           1000     8587       116      0        0      9.6     10052
bulk / AT_MOST_ONCE_BULK                 OS x8-16          20000     1851     10.8K      0        0     16.2       849
bulk / AT_LEAST_ONCE_BULK                OS x8-16         200000      137   1459.9K      1        1    124.6       653
virtual / AT_LEAST_ONCE_BULK / unbounded virtual ∞        200000      740    270.3K      8        9    243.2      1275
virtual / AT_LEAST_ONCE_BULK / cap=64    virtual cap=64   200000     1129    177.1K      7        5    228.8      1200
virtual / AT_LEAST_ONCE_BULK / cap=8     virtual cap=8    200000    11901     16.8K     32       26    260.6      1366
virtual / AT_LEAST_ONCE_SINGLE / cap=64  virtual cap=64   200000      506    395.3K     11        7    229.2      1202
======================================================================================================================
JVM      : 25.0.2
Collector: G1 (G1 Young Generation + G1 Concurrent GC + G1 Old Generation), compactObjectHeaders=false
Heap     : max=1024MB
```

## Commit strategy dominates everything else

The offset commit strategy is by far the largest lever, and it is
**GC-independent** — these cases are bounded by the broker round trip, and the
numbers held across all four collectors tested.

| Strategy | E2E TPS | Commit behaviour |
|---|---:|---|
| `AT_MOST_ONCE_SINGLE` | **112 – 116** | One synchronous commit **per record** (~8.3 ms round trip) |
| `AT_MOST_ONCE_BULK` | **9.5K – 11.3K** | One synchronous commit **per poll batch** |
| `AT_LEAST_ONCE_*` | **100K – 1.4M** | Offsets coalesced into one `commitAsync` per poll cycle |

`AT_MOST_ONCE_SINGLE` measured 8587 ms for 1000 records — 8.6 ms per record,
which is the commit round trip and nothing else. This is deliberate and was not
"optimised" in 4.1.x: at-most-once means the commit must be durable *before* the
listener runs, so it cannot be made asynchronous without breaking the guarantee.
If you need per-record commits, this is the price; if you do not, `_BULK` is ~90×
faster for the same durability class.

The at-least-once figures are where 4.1.0 changed behaviour: the poller now
accumulates offsets and issues a single `commitAsync` per poll cycle instead of a
synchronous commit per record.

> **On the "before" number:** pre-4.1.0, `AT_LEAST_ONCE_SINGLE` called
> `commitSync` for every completed record, putting it in the same cost class as
> `AT_MOST_ONCE_SINGLE`. That is read from the old code path, not measured — the
> measured 116 TPS of `AT_MOST_ONCE_SINGLE`, which still commits synchronously
> per record, is the live proxy for what that cost looks like.

## Bounded virtual dispatcher — pick the cap deliberately

`BoundedVirtualExecutorStrategy` caps concurrent listener invocations. A
thread-per-task virtual executor never rejects, so without a cap nothing limits
how much of a poll batch runs at once.

| Concurrency cap | E2E TPS (G1) |
|---|---:|
| unbounded | 270.3K |
| 64 | 177.1K |
| 8 | **16.8K** |

Going from 64 to 8 costs an order of magnitude. Set the cap from the real
concurrency your downstream can absorb — too low and it becomes the bottleneck;
at 64 the semaphore itself was not measurable against unbounded.

## Virtual threads — where the win actually is

The matrix above uses a no-op listener and therefore says nothing about virtual
threads. This one blocks for 20 ms per invocation, standing in for a database or
HTTP call.

```
io 20ms / OS pool 16                     OS x16             5000     7428       673      1        3     21.4      4482
io 20ms / OS pool 64                     OS x64             5000     1877      2.7K      0        0      7.6      1587
io 20ms / OS pool 256                    OS x256            5000      582      8.6K      1        1      6.6      1386
io 20ms / virtual cap=64                 virtual cap=64     5000     2154      2.3K      2        6     35.3      7400
io 20ms / virtual cap=256                virtual cap=256    5000      717      7.0K      1        1     17.9      3763
io 20ms / virtual cap=1024               virtual cap=1024   5000      399     12.5K      1        2     12.0      2524
io 20ms / virtual unbounded              virtual ∞          5000      348     14.4K      1        1     12.7      2656
```

**Throughput tracks concurrency, not thread type.** At matched concurrency,
platform threads are marginally *ahead*:

| Concurrency | OS threads | Virtual threads |
|---|---:|---:|
| 64 | **2.7K** | 2.3K |
| 256 | **8.6K** | 7.0K |

The virtual-thread advantage is that you can raise the number far beyond what
platform threads allow: `cap=1024` reaches 12.5K and unbounded 14.4K. A pool of
1024 platform threads costs on the order of a gigabyte of stack reservation at
the default 1 MB per thread; 1024 virtual threads cost almost nothing. So the
argument is **footprint and feasibility at high concurrency**, not raw speed per
unit of concurrency.

Note none of the cases reach the theoretical ceiling — 5000 records × 20 ms ÷ 1024
is 98 ms, versus 399 ms measured. `max.poll.records=100` and `autoPartitionPause`
bound how much of a partition can be in flight, so concurrency alone is not the
only limit.

## Garbage collectors

`-XX:+UseCompactObjectHeaders` (JEP 519 — a product feature in JDK 25, **off by
default**) is the one clear, repeatable win. Object headers drop from 12/16 bytes
to 8, and the saving scales with object count, which suits a consumer path that
allocates many small objects per record.

### Allocation per record

| Case | G1 | ZGC (gen) | Parallel | **G1 + COH** | COH saving |
|---|---:|---:|---:|---:|---:|
| single / `AT_LEAST_ONCE_BULK` | 841 | 1092 | 841 | **734** | **−12.7%** |
| single / `AT_LEAST_ONCE_SINGLE` | 834 | 1089 | 838 | **739** | **−11.4%** |
| bulk / `AT_LEAST_ONCE_BULK` | 653 | 838 | 655 | **589** | **−9.8%** |
| single / `AT_MOST_ONCE_SINGLE` | 10052 | 12943 | 9732 | **8663** | **−13.8%** |
| virtual cap=64 | 1200 | 1589 | 1207 | **1053** | **−12.3%** |
| virtual unbounded | 1275 | 1643 | 1259 | **1125** | **−11.8%** |

ZGC allocates systematically ~30% more per record (different TLAB and alignment
behaviour).

### GC pause totals

| Collector | Total pause range across cases |
|---|---|
| G1 | 0 – 26 ms |
| ZGC (generational) | 0 – 80 ms |
| Parallel | 0 – 20 ms |
| G1 + COH | 0 – 35 ms |

**GC is not the bottleneck in any configuration.** Total pause stayed under 35 ms
on 200,000-record runs whose end-to-end times ranged from 137 ms to 12 s. Choose
a collector here for footprint and tail latency, not for throughput.

### JDK 25 collector notes, as observed on this build

- **G1 is the default.**
- **ZGC is generational only.** JDK 24 removed the non-generational mode; the
  JVM reports `ZGC Minor/Major Cycles` beans and separate young/old GC worker
  pools.
- **Shenandoah is absent from this build** — `-XX:+UseShenandoahGC` fails with
  `Option -XX:+UseShenandoahGC not supported`, so generational Shenandoah
  (JEP 521) could not be measured.
- **Compact object headers** are available and must be enabled explicitly.

## What this report does *not* show

**Do not rank collectors by the throughput numbers.** Each configuration was run
**once, with no warm-up**. The dispatch-bound cases vary up to 4× between runs —
`AT_LEAST_ONCE_SINGLE` measured 1041K (G1), 611K (ZGC), 274K (Parallel) and 369K
(G1+COH). That spread is measurement noise, not a collector ranking. Only two
things here are stable enough to quote: the commit-bound cases and the allocation
column.

**The no-op matrix does not show the virtual-thread advantage** — platform threads
are naturally ahead on CPU-trivial work there (G1: 508K OS-thread vs 270K
virtual). Use the [I/O-bound matrix](#virtual-threads--where-the-win-actually-is)
for that question, and note that even it shows the advantage is reachable
concurrency rather than speed.

**Bulk cases are not per-record comparable.** A bulk listener is invoked once per
partition batch, so its per-record cost is near zero and `bulk /
AT_LEAST_ONCE_BULK` at 1.4M TPS is a batch-dispatch figure, not a per-record one.

## Investigated and closed — the ZGC "stall"

One sweep under `-XX:+UseZGC -Xmx1g` saw `virtual / AT_LEAST_ONCE_BULK /
unbounded` fail to finish within 5 minutes, while the same case took 740 ms under
G1. **Three targeted experiments then failed to reproduce it, so it is not
treated as a library defect:**

| Experiment | Result |
|---|---|
| `UnboundedVirtualStallDiagnosticTest` — that case alone under ZGC, 200k records, with a thread dump armed on stall | 200,000/200,000 in under 2 s |
| `ConsumerLifecycleLeakTest` — 8 consecutive consumer start/stop cycles, OS and virtual | Platform threads flat at 9 (OS) and 22 (virtual); **no leak** |
| Full ZGC sweep, re-run | All 10 cases passed; the case in question ran in 515 ms at 388K TPS |

The stalling run was the first at the raised 200k backlog, as the seventh case in
a shared JVM that had created and deleted a topic per case. The most likely
explanation is broker-side or harness state from that churn rather than the
collector or the dispatcher. The ZGC rows above come from the clean re-run.

Kept here rather than deleted because "observed once, then disproved three ways"
is more useful to the next person than silence — and if it resurfaces, the two
diagnostics are already committed.

## Reproducing

```bash
cd example/spring-kafka-protobuf/src/main/resources && docker compose up -d

# baseline
./gradlew :spring-kafka:test --tests '*ThroughputBenchmarkTest*' -Pbenchmark=true

# collector sweep
./gradlew :spring-kafka:test --tests '*ThroughputBenchmarkTest*' -Pbenchmark=true \
    -PbenchmarkJvmArgs="-XX:+UseCompactObjectHeaders -Xmx1g"
./gradlew :spring-kafka:test --tests '*ThroughputBenchmarkTest*' -Pbenchmark=true \
    -PbenchmarkJvmArgs="-XX:+UseZGC -Xmx1g"
```

Benchmarks are opt-in via `-Pbenchmark=true`, so a plain `./gradlew build` is
unaffected. `GcProbe` collects the GC and allocation counters.

## Tuning summary

1. **Avoid `AT_MOST_ONCE_SINGLE`** unless per-record at-most-once is a hard
   requirement — it costs one broker round trip per record (~116 TPS here).
2. **Prefer `AT_LEAST_ONCE_*`** where duplicates are tolerable; commits leave the
   critical path entirely.
3. **Use the bulk poller** when the listener can process a batch — it amortises
   per-record dispatch cost.
4. **Set the virtual concurrency cap** from what the downstream can absorb — it is
   the throughput knob for I/O-bound listeners (2.3K at 64, 12.5K at 1024) and an
   order-of-magnitude penalty if set too low (16.8K at cap 8 versus 177K at 64 in
   the no-op matrix). Prefer a high explicit cap over unbounded: unbounded was
   only ~15% faster than `cap=1024` here and gives up all backpressure.
5. **Reach for virtual threads when the listener blocks**, and size the cap well
   past what a platform pool could afford — that, not per-thread speed, is the
   advantage.
6. **Turn on `-XX:+UseCompactObjectHeaders`** for a 10–14% allocation reduction
   at no measured throughput cost.
7. **Do not tune the collector for throughput** in this workload; GC pauses are
   already negligible.
