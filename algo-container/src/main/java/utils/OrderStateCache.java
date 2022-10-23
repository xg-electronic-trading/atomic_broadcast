package utils;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import container.MutableOrderState;
import org.agrona.collections.Long2LongHashMap;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.util.Map;

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

public class OrderStateCache implements StateCache {

    private final int NULL_VALUE = -1;
    private final int BYTES_PER_KB = 1024;
    private final int BYTES_PER_MB = BYTES_PER_KB * BYTES_PER_KB;
    private long entryIndex = 0;

    private final ObjectPool<MutableOrderState> pool;

    private final Long2LongHashMap id2Index = new Long2LongHashMap(10_000, 0.55f, -1, true);

    private final OrderStateFlyweight flyweight;
    private final OrderStateField[] fields;

    public OrderStateCache(boolean offHeap, int cacheSizeMb, ObjectPool<MutableOrderState> pool, OrderStateField[] fields) {
        this.fields = fields;
        this.pool = pool;

        if (offHeap) {
            flyweight = new OrderStateFlyweight(offHeap, cacheSizeMb * BYTES_PER_MB, fields);
        } else {
            flyweight = new OrderStateFlyweight(offHeap, cacheSizeMb *  BYTES_PER_MB, fields);
        }
    }

    @Override
    public MutableOrderState orderState(long id) {
        long index = findAndSetObjectOffset(id);
        MutableOrderState os = pool.construct();

        if (index != NULL_VALUE) {
            //get object from pool and populate using flyweight
            populateState(os);
            return os;
        } else {
            //add new index and create a new orderstate entry.
            entryIndex++;
            id2Index.put(id, entryIndex);
            os.orderId = id;
            return os;
        }
    }

    @Override
    public void commitState(MutableOrderState state) {
        long index = findAndSetObjectOffset(state.orderId());
        checkIndexNotNull(index);

        int fieldOffset = 0;
        for (int i = 0; i < fields.length; i++) {
            OrderStateField field = fields[i];
            switch (field.getType()) {
                case IntType:
                    flyweight.set(state.getInt(field), fieldOffset);
                    break;
                case LongType:
                    flyweight.set(state.getLong(field), fieldOffset);
                    break;
                default:
                    throw new IllegalArgumentException("field datatype not supported: " + field);
            }
            fieldOffset += field.getSize();
        }

        state.destruct();
        flyweight.setObjectOffset(0); //reset offset to beginning
    }

    @Override
    public void commitField(long id, OrderStateField field, short value) {
        int fieldOffset = findFieldOffset(id, field);
        flyweight.set(value, fieldOffset);

    }

    @Override
    public void commitField(long id, OrderStateField field, int value) {
        int fieldOffset = findFieldOffset(id, field);
        flyweight.set(value, fieldOffset);
    }

    @Override
    public void commitField(long id, OrderStateField field, long value) {
        int fieldOffset = findFieldOffset(id, field);
        flyweight.set(value, fieldOffset);
    }

    @Override
    public void commitField(long id, OrderStateField field, boolean value) {

    }

    @Override
    public void commitField(long id, OrderStateField field, byte value) {

    }

    @Override
    public void commitField(long id, OrderStateField field, CharSequence value) {

    }

    private void populateState(MutableOrderState os) {
        int fieldOffset = 0;
        for (int i = 0; i < fields.length; i++) {
            OrderStateField field = fields[i];
            switch (field.getType()) {
                case IntType:
                    os.set(field, flyweight.getInt(fieldOffset));
                    break;
                case LongType:
                    os.set(field, flyweight.getLong(fieldOffset));
                    break;
                default:
                    throw new IllegalArgumentException("field datatype not supported: " + field);
            }
            fieldOffset += field.getSize();
        }
    }

    private int findFieldOffset(long id, OrderStateField fieldToCommit) {
        long index = findAndSetObjectOffset(id);
        checkIndexNotNull(index);

        int fieldOffset = 0;
        for (int i = 0; i < fields.length; i++) {
            OrderStateField field = fields[i];
            if (fieldToCommit == field) {
                return fieldOffset;
            }
            fieldOffset += field.getSize();
        }

        return NULL_VALUE;
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

    public void close() {
        id2Index.clear();
        flyweight.close();
    }


}
