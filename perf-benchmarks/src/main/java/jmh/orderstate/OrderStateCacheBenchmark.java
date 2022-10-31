package jmh.orderstate;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import orderstate.ByteBufferOrderStateCache;
import orderstate.OrderState;
import orderstate.OrderStateField;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import pool.ObjectPoolDefinitions;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class OrderStateCacheBenchmark {

    private static final Log log = LogFactory.getLog(OrderStateCacheBenchmark.class.getName());

    @Param({"true", "false"})
    private boolean offHeap;

    private ByteBufferOrderStateCache cache;

    @Setup(Level.Iteration)
    public void setup() {
        ObjectPoolDefinitions pools = new ObjectPoolDefinitions();
        OrderStateField[] fields = new OrderStateField[] {
                OrderStateField.Id,
                OrderStateField.Price,
                OrderStateField.Quantity
        };
        cache = new ByteBufferOrderStateCache(offHeap, 30, fields);

        for (int i = 0; i < cache.maxOrders() - 1; i++) {
            cache.orderState(i);
            cache.setPrice(i);
            cache.setQuantity(i * 10L);
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
        OrderState os = cache.orderState(orderId);
        cache.setQuantity(10);
        cache.setPrice(10);

        blackhole.consume(cache);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 5)
    @Measurement(iterations = 5)
    public void testReadEntireCache(Blackhole blackhole) {
        for (int i = 0; i < cache.maxOrders() - 1; i++) {
            OrderState state = cache.orderState(i);
            blackhole.consume(state.orderId());
            blackhole.consume(state.price());
            blackhole.consume(state.quantity());
        }

        blackhole.consume(cache);
    }
}
