package com.inomera.telco.commons.springkafka.e2e;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;

/**
 * Captures GC and allocation counters around a benchmark case.
 *
 * <p>Collector names differ per GC (G1 reports "G1 Young Generation"/"G1 Old Generation", ZGC
 * reports "ZGC Minor/Major Cycles" and separate "Pauses" beans), so the probe sums across all
 * beans rather than assuming a layout.
 *
 * <p>Allocation comes from {@code com.sun.management.ThreadMXBean#getTotalThreadAllocatedBytes},
 * which counts every thread including the virtual ones the invoker uses.
 */
final class GcProbe {

    private final long collections;
    private final long pauseMillis;
    private final long allocatedBytes;
    private final long heapUsedBytes;

    private GcProbe(long collections, long pauseMillis, long allocatedBytes, long heapUsedBytes) {
        this.collections = collections;
        this.pauseMillis = pauseMillis;
        this.allocatedBytes = allocatedBytes;
        this.heapUsedBytes = heapUsedBytes;
    }

    static GcProbe capture() {
        long collections = 0;
        long pauseMillis = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            final long count = bean.getCollectionCount();
            final long time = bean.getCollectionTime();
            if (count > 0) {
                collections += count;
            }
            if (time > 0) {
                pauseMillis += time;
            }
        }
        final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        return new GcProbe(collections, pauseMillis, totalAllocatedBytes(),
                memory.getHeapMemoryUsage().getUsed());
    }

    /** Deltas between this snapshot and a later one. */
    Delta since(GcProbe start) {
        return new Delta(this.collections - start.collections,
                this.pauseMillis - start.pauseMillis,
                Math.max(0L, this.allocatedBytes - start.allocatedBytes),
                this.heapUsedBytes);
    }

    record Delta(long collections, long pauseMillis, long allocatedBytes, long heapUsedAfterBytes) {
        double allocatedMb() {
            return allocatedBytes / (1024.0d * 1024.0d);
        }

        double allocatedBytesPerRecord(int records) {
            return records <= 0 ? 0 : (double) allocatedBytes / records;
        }

        double heapUsedAfterMb() {
            return heapUsedAfterBytes / (1024.0d * 1024.0d);
        }
    }

    /** Collector names as reported by the JVM, e.g. "G1 Young Generation + G1 Old Generation". */
    static String activeCollectors() {
        final List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        return beans.stream()
                .map(GarbageCollectorMXBean::getName)
                .filter(name -> !name.endsWith("Pauses"))
                .reduce((a, b) -> a + " + " + b)
                .orElse("unknown");
    }

    /** Short label for the report: G1, ZGC, Parallel, Serial or Shenandoah. */
    static String collectorLabel() {
        final String names = String.join(",", ManagementFactory.getGarbageCollectorMXBeans().stream()
                .map(GarbageCollectorMXBean::getName).toList());
        if (names.contains("ZGC")) {
            return "ZGC(gen)";
        }
        if (names.contains("G1")) {
            return "G1";
        }
        if (names.contains("Shenandoah")) {
            return "Shenandoah";
        }
        if (names.contains("PS ")) {
            return "Parallel";
        }
        if (names.contains("Copy") || names.contains("MarkSweepCompact")) {
            return "Serial";
        }
        return names;
    }

    static boolean compactObjectHeaders() {
        // Reflected off the VM flag so the probe does not need the HotSpot management module.
        try {
            final var mbeans = ManagementFactory.getPlatformMBeanServer();
            final var attr = mbeans.invoke(new javax.management.ObjectName("com.sun.management:type=HotSpotDiagnostic"),
                    "getVMOption", new Object[]{"UseCompactObjectHeaders"}, new String[]{String.class.getName()});
            return attr != null && String.valueOf(attr).contains("value=true");
        } catch (Exception e) {
            return false;
        }
    }

    private static long totalAllocatedBytes() {
        try {
            final var threadBean = ManagementFactory.getThreadMXBean();
            if (threadBean instanceof com.sun.management.ThreadMXBean sunBean
                    && sunBean.isThreadAllocatedMemorySupported()) {
                return sunBean.getTotalThreadAllocatedBytes();
            }
        } catch (Exception | NoClassDefFoundError e) {
            // Not available: allocation columns will read zero.
        }
        return 0L;
    }
}
