package atomic_broadcast.utils;

import java.util.Random;

public class BoundedRandomNumberGenerator {

    private final double BoundPct = 0.25;
    private final Random random = new Random();
    private final long lowerbound;
    private final long upperbound;

    public BoundedRandomNumberGenerator(long seed) {
        this.lowerbound = calculateBound(seed, false);
        this.upperbound = calculateBound(seed, true);
    }

    private long calculateBound(long seed, boolean upperbound) {
        return upperbound ? Math.round(seed * (1 + BoundPct)) : Math.round(seed * (1 - BoundPct));
    }

    public long generateRandom() {
        return random.nextInt((int) (upperbound - lowerbound)) + lowerbound;
    }
}
