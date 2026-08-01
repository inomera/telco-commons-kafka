package com.inomera.telco.commons.springkafka.consumer.executor.virtual;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;

/**
 * Guards the {@link ThreadFactory} contract for the virtual executor strategies.
 *
 * <p>{@code ThreadFactory.newThread} must return an <em>unstarted</em> thread: every
 * {@code Executors.*} factory calls {@code start()} on whatever it gets back. A factory that
 * starts the thread itself makes the executor throw {@link IllegalThreadStateException} on every
 * single task, which surfaces as "Error dispatching..." for each record and silently drops the
 * work.
 *
 * <p>This is easy to get wrong in practice: {@code IncrementalNamingVirtualThreadFactory} in
 * telco-commons-lang builds its thread with {@code Thread.ofVirtual().name(..).start(runnable)}
 * and therefore cannot be handed to an executor — that is what its
 * {@code IncrementalNamingUnstartedVirtualThreadFactory} sibling is for.
 *
 * @author Turgay Can
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VirtualThreadFactories {
    private static final Logger LOG = LoggerFactory.getLogger(VirtualThreadFactories.class);

    /**
     * Returns {@code candidate} when it honours the contract, otherwise a named virtual thread
     * factory that does.
     *
     * @param candidate      caller-supplied factory, may be {@code null}
     * @param fallbackPrefix thread name prefix used when falling back
     */
    public static ThreadFactory unstartedVirtual(ThreadFactory candidate, String fallbackPrefix) {
        if (candidate == null) {
            return namedVirtualFactory(fallbackPrefix);
        }
        if (returnsUnstartedThreads(candidate)) {
            return candidate;
        }
        LOG.warn("Thread factory {} returns already-started threads, which violates the ThreadFactory "
                        + "contract and would make every dispatched task fail with "
                        + "IllegalThreadStateException. Falling back to an unstarted virtual thread factory "
                        + "named '{}'. Use an 'unstarted' factory variant instead.",
                candidate.getClass().getName(), fallbackPrefix);
        return namedVirtualFactory(fallbackPrefix);
    }

    private static ThreadFactory namedVirtualFactory(String prefix) {
        return Thread.ofVirtual().name(prefix == null ? "virtual-invoker-" : prefix, 0).factory();
    }

    /**
     * Probes the factory once with a no-op task. A conforming factory hands back a thread in
     * {@link Thread.State#NEW}; a non-conforming one has already run or is running it.
     */
    private static boolean returnsUnstartedThreads(ThreadFactory candidate) {
        try {
            final Thread probe = candidate.newThread(() -> {
            });
            if (probe == null) {
                return false;
            }
            if (probe.getState() != Thread.State.NEW) {
                return false;
            }
            // Conforming factory: consume the probe so it does not linger unstarted.
            probe.start();
            return true;
        } catch (RuntimeException e) {
            LOG.warn("Thread factory {} could not be probed: {}", candidate.getClass().getName(), e.getMessage());
            return false;
        }
    }
}
