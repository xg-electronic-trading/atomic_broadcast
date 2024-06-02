package orderstate;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import org.agrona.BufferUtil;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

public class OrderStateFlyweight implements OrderState, OrderStateSetter {

    Log log = LogFactory.getLog(this.getClass().getName());

    private final UnsafeBuffer buffer;
    private final OrderStateField[] fields;
    private int objectSize = 0;
    private int objectOffset = 0;
    private int maxOrders;

    private int idOffset = -1;
    private int priceOffset = -1;
    private int quantityOffset = -1;
    private int msgSeqNumOffset = -1;

    public OrderStateFlyweight(boolean offHeap, int cacheSizeBytes, OrderStateField[] fields) {
        this.buffer = offHeap ? new UnsafeBuffer(ByteBuffer.allocateDirect(cacheSizeBytes)) : new UnsafeBuffer(ByteBuffer.allocate(cacheSizeBytes));
        this.fields = fields;

        for (int i = 0; i < fields.length; i++) {
            OrderStateField field = fields[i];
            switch (field) {
                case Id:
                    idOffset = objectSize;
                    break;
                case Price:
                    priceOffset = objectSize;
                    break;
                case Quantity:
                    quantityOffset = objectSize;
                    break;
                case MsgSeqNum:
                    msgSeqNumOffset = objectOffset;
                    break;
                default:
                    throw new IllegalArgumentException("field " + field + " not recognised and unable to be added to orderstate cache.");



            }
            objectSize += field.getSize();
        }

        maxOrders = cacheSizeBytes/objectSize;

        log.info().append("orderstate cache instantiated with cache size (bytes): ")
                .append(cacheSizeBytes)
                .append(", object size: ")
                .append(objectSize)
                .append(", order capacity: ")
                .appendLast(maxOrders);

    }

    public void setObjectOffset(int index) {
        objectOffset = index * objectSize;
    }

    public void setOrderId(long id) {
        set(id, idOffset);
    }

    public void setPrice(long price) {
        set(price, priceOffset);
    }

    public void setQuantity(long quantity) {
        set(quantity, quantityOffset);
    }

    public void setMsgSeqNum(long msgSeqNum) {
        set(msgSeqNum, msgSeqNumOffset);
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

    public int maxOrders() {
        return maxOrders;
    }


    @Override
    public long orderId() {
        return getLong(idOffset);
    }

    @Override
    public long quantity() {
        return getLong(quantityOffset);
    }

    @Override
    public long price() {
        return getLong(priceOffset);
    }

    @Override
    public long msgSeqNum() {
        return getLong(msgSeqNumOffset);
    }
}
