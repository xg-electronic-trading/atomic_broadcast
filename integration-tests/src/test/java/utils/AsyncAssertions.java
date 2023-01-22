package utils;

import atomic_broadcast.utils.CompositeModule;
import atomic_broadcast.utils.Pollable;
import atomic_broadcast.utils.TransportState;

import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.fail;

public class AsyncAssertions {
    private static final long AssertionTimeOutMs = 10_000L;

    public static void pollUntil(
            List<Pollable> pollables,
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
                    pollables.forEach(Pollable::poll);
                }
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    public static void pollUntil(
            List<Pollable> pollables,
            Supplier<Boolean> actual
    ) {
        try {
            long start = System.currentTimeMillis();
            while (!actual.get()) {
                long end = System.currentTimeMillis();
                if (end - start > AssertionTimeOutMs) {
                    throw new TimeoutException("timed out waiting for assertion");
                } else {
                    pollables.forEach(Pollable::poll);
                }
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }
}
