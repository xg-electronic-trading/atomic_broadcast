package time;

import org.junit.jupiter.api.Test;

public class ClockTest {

    @Test
    public void assertLdnTimeZone() {
        SimulatedClock clock = new SimulatedClock();
        clock.time();
    }

    @Test
    public void assertNycTimeZone() {
        SimulatedClock clock = new SimulatedClock("America/New_York");
        clock.time();
    }
}
