package atomic_broadcast.utils;

import java.util.Random;

public class BoundedRandomNumberGenerator {

    private final Random random = new Random();
    private final long lowerbound;
    private final long upperbound;

    public BoundedRandomNumberGenerator(long lowerbound, long seed, long upperbound) {
        this.lowerbound = lowerbound;
        this.upperbound = upperbound;
        random.setSeed(seed);
    }

    public long generateRandom() {
        return random.nextInt((int) (upperbound - lowerbound)) + lowerbound;
    }
}
