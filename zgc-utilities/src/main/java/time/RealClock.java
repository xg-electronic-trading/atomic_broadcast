package time;

import org.agrona.concurrent.EpochClock;
import org.agrona.concurrent.EpochMicroClock;
import org.agrona.concurrent.EpochNanoClock;

public class RealClock implements EpochClock, EpochNanoClock, EpochMicroClock {

    @Override
    public long time() {
        return System.currentTimeMillis();
    }

    @Override
    public long nanoTime() {
        return microTime() * 1_000L;
    }

    @Override
    public long microTime() {
        return time() * 1_000L;
    }
}
