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
            OrderStateFlyweight state = cache.orderState(i);
            state.setOrderId(i);
            state.setPrice(i);
            state.setQuantity(i * 10L);
        }
    }

    @AfterEach
    public void after() {
        cache.close();
    }


    @Test
    public void startStopCache() {
        long orderId = 1L;
        OrderStateFlyweight state = cache.orderState(orderId);
        state.setQuantity(10);
        state.setPrice(10);


        OrderState readOs = cache.orderState(orderId);

        assertEquals(orderId, readOs.orderId());
        assertEquals(10, readOs.quantity());

        state.setPrice(11);

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
