package time;

import org.agrona.concurrent.EpochClock;
import org.agrona.concurrent.NanoClock;

public interface Clock extends NanoClock, EpochClock {
}
