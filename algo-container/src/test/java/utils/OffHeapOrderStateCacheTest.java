package utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

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
    public void startStopCache() throws IOException {
    }
}
