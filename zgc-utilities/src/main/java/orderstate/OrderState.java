package orderstate;

public interface OrderState {

    long orderId();

    long quantity();

    long price();

    long msgSeqNum();

}
