package orderstate;

public interface OrderState {

    long orderId();

    boolean shouldTrigger();

    long quantity();

    long price();

    long msgSeqNum();

}
