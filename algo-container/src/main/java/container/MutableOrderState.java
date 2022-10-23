package container;

import utils.AbstractPooledObject;
import utils.OrderStateField;

public class MutableOrderState extends AbstractPooledObject implements OrderState, OrderTrigger {

    private boolean shouldTrigger;
    public long orderId;
    public long price;
    public long quantity;
    public int msgSeqNum;

    public MutableOrderState() {
        reset();
    }

    @Override
    public long orderId() {
        return 0;
    }

    @Override
    public boolean shouldTrigger() {
        return false;
    }

    @Override
    public void markOrderToRun() {
        this.shouldTrigger = true;
    }

    public long getLong(OrderStateField field) {
        switch (field) {
            case Id:
                return orderId;
            case Price:
                return price;
            case Quantity:
                return quantity;
            default:
                throw new IllegalArgumentException("field not found in order state: " + field);
        }
    }

    public int getInt(OrderStateField field) {
        switch (field) {
            case MsgSeqNum:
                return msgSeqNum;
            default:
                throw new IllegalArgumentException("field not found in order state: " + field);
        }
    }

    public void set(OrderStateField field, long value) {
        switch (field) {
            case Id:
                orderId = value;
                break;
            case Price:
                price = value;
                break;
            case Quantity:
                quantity = value;
                break;
            default:
                throw new IllegalArgumentException("field not found in order state: " + field);
        }
    }

    public void set(OrderStateField field, int value) {
        switch (field) {
            case MsgSeqNum:
                msgSeqNum = value;
                break;
            default:
                throw new IllegalArgumentException("field not found in order state: " + field);
        }
    }

    @Override
    public void reset() {
        shouldTrigger = false;
        price = -1;
        quantity = -1;
        orderId = -1;
        msgSeqNum = -1;

    }
}
