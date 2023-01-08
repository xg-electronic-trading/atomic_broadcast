package utils;

import atomic_broadcast.utils.CompositeModule;
import atomic_broadcast.utils.TransportState;

import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.fail;

public class AsyncAssertions {
    private static final long AssertionTimeOutMs = 10_000L;

    public static void pollUntil(
            CompositeModule modules,
            TransportState expected,
            Supplier<TransportState> actual
    ) {
        try {
            long start = System.currentTimeMillis();
            while (expected != actual.get()) {
                long end = System.currentTimeMillis();
                if (end - start > AssertionTimeOutMs) {
                    throw new TimeoutException("timed out waiting for assertion");
                } else {
                    modules.poll();
                }
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    public static void pollUntil(
            CompositeModule modules,
            Supplier<Boolean> actual
    ) {
        try {
            long start = System.currentTimeMillis();
            while (!actual.get()) {
                long end = System.currentTimeMillis();
                if (end - start > AssertionTimeOutMs) {
                    throw new TimeoutException("timed out waiting for assertion");
                } else {
                    modules.poll();
                }
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }
}
