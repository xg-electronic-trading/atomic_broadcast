package container;

public interface OrderState {

    long orderId();

    boolean shouldTrigger();

}
