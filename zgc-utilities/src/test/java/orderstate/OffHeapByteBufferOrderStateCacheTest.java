package orderstate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pool.ObjectPoolDefinitions;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OffHeapByteBufferOrderStateCacheTest {

    ByteBufferOrderStateCache cache;

    @BeforeEach
    public void before() {
        ObjectPoolDefinitions objectPoolDefinitions = new ObjectPoolDefinitions();
        OrderStateField[] fields = new OrderStateField[] {
                OrderStateField.Id,
                OrderStateField.Price,
                OrderStateField.Quantity
        };

        cache = new ByteBufferOrderStateCache(true, 1, fields);

        for (int i = 0; i < cache.maxOrders() - 1; i++) {
            OrderState state = cache.orderState(i);
            cache.setId(i);
            cache.setPrice(i);
            cache.setQuantity(i * 10L);
        }
    }

    @AfterEach
    public void after() {
        cache.close();
    }


    @Test
    public void startStopCache() {
        long orderId = 1L;
        cache.orderState(orderId);
        cache.setQuantity(10);
        cache.setPrice(10);


        OrderState readOs = cache.orderState(orderId);

        assertEquals(orderId, readOs.orderId());
        assertEquals(10, readOs.quantity());

        cache.setPrice(11);

        assertEquals(11, readOs.price());
    }

    @Test
    public void readEntireCache() {
        for (int i = 0; i < cache.maxOrders() - 1; i++) {
            OrderState os = cache.orderState(i);
            assertEquals(i, os.orderId());
            assertEquals(i, os.price());
        }
    }
}
