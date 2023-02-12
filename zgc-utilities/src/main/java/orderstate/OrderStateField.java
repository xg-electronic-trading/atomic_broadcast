package orderstate;

import static orderstate.DataType.IntType;
import static orderstate.DataType.LongType;

public enum OrderStateField {

    Id(0, LongType, Long.BYTES),
    Price(1, LongType, Long.BYTES),
    Quantity(2, LongType, Long.BYTES),
    MsgSeqNum(3, LongType, Long.BYTES);


    private final int code;
    private final DataType type;
    private final int size;

    OrderStateField(int code, DataType type, int size) {
        this.code = code;
        this.type = type;
        this.size = size;
    }

    public int getCode() {
        return code;
    }

    public DataType getType() {
        return type;
    }

    public int getSize() {
        return size;
    }
}
