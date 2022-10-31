package orderstate;

import org.agrona.SystemUtil;
import org.agrona.collections.Long2LongHashMap;
import pool.ObjectPool;

/**
 * OrderStateCache backed by either a on-heap or off-heap buffer.
 * The general design of the cache is to store all state data in
 * a continguos block of memory which is then more performant
 * when reading and writing the data due to the fact there
 * will be a predictable access pattern to the data.
 * i.e. the CPU can pre-fetch data into the cache.
 *
 * This will be particularly beneficial when iterating
 * through orders and triggering algos.
 */

public class ByteBufferOrderStateCache implements StateCache, OrderStateSetter {

    private final int NULL_VALUE = -1;
    private final int BYTES_PER_KB = 1024;
    private final int BYTES_PER_MB = BYTES_PER_KB * BYTES_PER_KB;
    private long entryIndex = 0;

    private final Long2LongHashMap id2Index = new Long2LongHashMap(4_000_000, 0.55f, -1, true);

    private final OrderStateFlyweight flyweight;

    public ByteBufferOrderStateCache(boolean offHeap, int cacheSizeMb, OrderStateField[] fields) {
        this.flyweight = new OrderStateFlyweight(offHeap, cacheSizeMb *  BYTES_PER_MB, fields);
    }

    @Override
    public OrderState orderState(long id) {
        long index = findAndSetObjectOffset(id);

        if (index != NULL_VALUE) {
            return flyweight;
        } else {
            //add new index and create a new orderstate entry.
            id2Index.put(id, entryIndex);
            flyweight.setObjectOffset((int) entryIndex);
            flyweight.setOrderId(id);
            entryIndex++;

            return flyweight;
        }
    }

    private long findAndSetObjectOffset(long id) {
        long index = id2Index.get(id);
        if (index != NULL_VALUE) {
            flyweight.setObjectOffset((int) index);
            return index;
        }

        return NULL_VALUE;
    }

    private void checkIndexNotNull(long index) {
        assert index != NULL_VALUE;
    }

    public int maxOrders() {
        return flyweight.maxOrders();
    }

    public void close() {
        id2Index.clear();
        flyweight.close();
    }


    @Override
    public void setId(long id) {
        flyweight.setOrderId(id);
    }

    @Override
    public void setPrice(long price) {
        flyweight.setPrice(price);
    }

    @Override
    public void setQuantity(long quantity) {
        flyweight.setQuantity(quantity);
    }

    @Override
    public void setMsgSeqNum(long msgSeqNum) {
        flyweight.setMsgSeqNum(msgSeqNum);
    }
}
