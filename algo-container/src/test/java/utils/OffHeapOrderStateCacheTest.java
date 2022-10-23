package utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OffHeapOrderStateCacheTest {

    StateCache osCache;

    @BeforeEach
    public void before() {
        ObjectPoolDefinitions objectPoolDefinitions = new ObjectPoolDefinitions();
        OrderStateField[] fields = new OrderStateField[] {
                OrderStateField.Id,
                OrderStateField.Price,
                OrderStateField.Quantity
        };

        osCache = new OrderStateCache(true, 1, objectPoolDefinitions.orderStateObjectPool, fields);

    }

    @AfterEach
    public void after() {
        osCache.close();
    }


    @Test
    public void startStopCache() {
        long orderId = 1L;
        MutableOrderState os = osCache.orderState(orderId);
        os.quantity = 10;
        os.price = 10;
        os.msgSeqNum = 1;

        osCache.commitState(os);


        MutableOrderState readOs = osCache.orderState(orderId);

        assertEquals(orderId, readOs.orderId());
        assertEquals(10, readOs.quantity());
        //msgSeqNum not stored as wasnt declared upfront as field to store
        assertEquals(-1, readOs.msgSeqNum());

        osCache.commitState(readOs);

        assertEquals(-1, readOs.orderId);
        assertEquals(-1, readOs.quantity);
        assertEquals(-1, readOs.price);
        assertEquals(-1, readOs.msgSeqNum);

        osCache.commitField(orderId, OrderStateField.Quantity, 12);

        readOs = osCache.orderState(orderId);

        assertEquals(orderId, readOs.orderId());
        assertEquals(12, readOs.quantity());
        assertEquals(-1, readOs.msgSeqNum());

        osCache.commitState(readOs);
    }
}
