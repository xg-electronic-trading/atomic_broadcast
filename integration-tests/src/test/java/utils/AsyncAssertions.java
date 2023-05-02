package utils;

import atomic_broadcast.utils.Pollable;
import atomic_broadcast.utils.TransportState;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import org.agrona.SystemUtil;

import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.fail;

public class AsyncAssertions {
    private static final Log log = LogFactory.getLog(AsyncAssertions.class.getName());

    private static final long AssertionTimeOutMs = SystemUtil.isDebuggerAttached() ? 60_000L * 5 : 30_000L;

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
                    pollables.forEach(p -> {
                        p.poll();
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }
}
