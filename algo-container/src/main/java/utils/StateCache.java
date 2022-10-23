package utils;

import container.MutableOrderState;

public interface StateCache {

    /**
     * This method returns an order state object from
     * an object pool which is populated from off heap buffer using a flyweight
     */

    MutableOrderState orderState(long id);

    /**
     * commit entire state object to off heap buffer
     * and return object to pool
     */
    void commitState(MutableOrderState state);

    void commitField(long id, OrderStateField field, short value);

    void commitField(long id, OrderStateField field, int value);

    void commitField(long id, OrderStateField field, long value);

    void commitField(long id, OrderStateField field, boolean value);

    void commitField(long id, OrderStateField field, byte value);

    void commitField(long id, OrderStateField field, CharSequence value);

    void close();
}
