package jmh.orderstate;

import orderstate.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import pool.ObjectPoolDefinitions;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class MapOrderStateCacheBenchmark {

    private MapOrderStateCache cache;
    private final int MAX_ORDERS = 1_310_000;

    @Setup(Level.Iteration)
    public void setup() {
        ObjectPoolDefinitions pools = new ObjectPoolDefinitions();
        cache = new MapOrderStateCache(pools.orderStateObjectPool);

        for (int i = 0; i < MAX_ORDERS; i++) {
            MutableOrderState state = cache.orderState(i);
            state.price = i;
            state.quantity = i * 10L;
            state.msgSeqNum = i;
            cache.commitState(state);
        }
    }

    @TearDown(Level.Iteration)
    public void teardown() {
        cache.close();
    }


    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 5)
    @Measurement(iterations = 5)
    public void testCommitNewOrderState(Blackhole blackhole) {
        long orderId = 1L;
        MutableOrderState os = cache.orderState(orderId);
        os.quantity = 10;
        os.price = 10;
        os.msgSeqNum = 1;

        cache.commitState(os);

        blackhole.consume(cache);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 5)
    @Measurement(iterations = 5)
    public void testReadEntireCache(Blackhole blackhole) {
        for (int i = 0; i < MAX_ORDERS; i++) {
            MutableOrderState os = cache.orderState(i);
            blackhole.consume(os.orderId());
            blackhole.consume(os.price());
            blackhole.consume(os.quantity());
        }

        blackhole.consume(cache);
    }
}
