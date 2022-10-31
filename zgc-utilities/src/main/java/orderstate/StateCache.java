package orderstate;

public interface StateCache {

    /**
     * This method returns an order state object from
     * an object pool which is populated from off heap buffer using a flyweight
     */

    OrderState orderState(long id);

    int maxOrders();

    void close();
}
