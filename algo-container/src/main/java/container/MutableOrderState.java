package container;

public class MutableOrderState implements OrderState, OrderTrigger {

    private boolean shouldTrigger = false;

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
}
