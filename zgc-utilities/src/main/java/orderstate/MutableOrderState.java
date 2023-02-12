package orderstate;

public class MutableOrderState extends AbstractPooledObject implements OrderState, OrderStateSetter {

    public long orderId;
    public long price;
    public long quantity;
    public long msgSeqNum;

    public MutableOrderState() {
        reset();
    }

    @Override
    public long orderId() {
        return orderId;
    }

    @Override
    public long quantity() {
        return quantity;
    }

    @Override
    public long price() {
        return price;
    }

    @Override
    public long msgSeqNum() {
        return msgSeqNum;
    }

    @Override
    public void reset() {
        price = -1;
        quantity = -1;
        orderId = -1;
        msgSeqNum = -1;

    }

    @Override
    public void setOrderId(long id) {
        this.orderId = id;
    }

    @Override
    public void setPrice(long price) {
        this.price = price;
    }

    @Override
    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    @Override
    public void setMsgSeqNum(long msgSeqNum) {
        this.msgSeqNum = msgSeqNum;
    }
}
