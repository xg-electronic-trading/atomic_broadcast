package utils;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import org.agrona.BufferUtil;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

public class OrderStateFlyweight {

    Log log = LogFactory.getLog(this.getClass().getName());

    private final UnsafeBuffer buffer;
    private final OrderStateField[] fields;
    private int objectSize = 0;
    private int objectOffset = 0;

    public OrderStateFlyweight(boolean offHeap, int cacheSizeBytes, OrderStateField[] fields) {
        this.buffer = offHeap ? new UnsafeBuffer(ByteBuffer.allocateDirect(cacheSizeBytes)) : new UnsafeBuffer(ByteBuffer.allocate(cacheSizeBytes));
        this.fields = fields;

        for (int i = 0; i < fields.length; i++) {
            OrderStateField field = fields[i];
            objectSize += field.getSize();
        }

        log.info().append("orderstate cache instantiated with cache size (bytes): ")
                .append(cacheSizeBytes)
                .append(", object size: ")
                .append(objectSize)
                .append(", order capacity: ")
                .appendLast(cacheSizeBytes/objectSize);
    }

    public void setObjectOffset(int index) {
        objectOffset = index * objectSize;
    }

    public void set(long value, int fieldOffset) {
        checkFieldOffsetIsInitialised(fieldOffset);
        buffer.putLong(objectOffset + fieldOffset, value);
    }

    public void set(int value, int fieldOffset) {
        checkFieldOffsetIsInitialised(fieldOffset);
        buffer.putInt(objectOffset + fieldOffset, value);
    }


    public long getLong(int fieldOffset) {
        checkFieldOffsetIsInitialised(fieldOffset);
        return buffer.getLong(objectOffset + fieldOffset);
    }

    public int getInt(int fieldOffset) {
        checkFieldOffsetIsInitialised(fieldOffset);
        return buffer.getInt(objectOffset + fieldOffset);
    }

    private void checkFieldOffsetIsInitialised(int offset) {
        assert offset != -1;
    }

    public void close() {
        BufferUtil.free(buffer);
    }


}
