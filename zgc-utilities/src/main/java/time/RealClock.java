package time;

public class RealClock implements Clock {

    @Override
    public long time() {
        return System.currentTimeMillis();
    }

    @Override
    public long nanoTime() {
        return time() * 1_000_000L;
    }
}
