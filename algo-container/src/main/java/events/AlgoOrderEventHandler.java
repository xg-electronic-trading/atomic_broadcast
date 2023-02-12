package events;

import orderstate.OrderState;
import orderstate.StateCache;
import schema.api.NewOrderSingle;
import schema.api.OrderCancelReplaceRequest;
import schema.api.OrderCancelRequest;

public class AlgoOrderEventHandler implements OrderEventHandler {

    private final StateCache osCache;

    public AlgoOrderEventHandler(StateCache osCache) {
        this.osCache = osCache;
    }

    /**
     * @param newOrder - this is a flyweight object that wraps around a buffer, providing direct
     *                 access to data. Accessing a field using the flyweight object will mean data
     *                 is decoded directly from the buffer using zero copy semantics. As this is a
     *                 flyweight that is owned by the MessageListener, the flyweight should NOT
     *                 be stored in a data structure for subsequent access. data should be read and
     *                 stored elsewhere (e.g. another object/buffer)
     *
     * On receipt of a new order, the algo event listener should perform the following steps:
     *                 1. read all data from the flyweight.
     *                 2. create a new state object to be stored in map or flyweight object
     *                 to be able write state to state buffer.
     *                 3. create data object associated with order id to store both state +
     *                 derived strategy data.
     *                 3. pass data object to strategy callback. (note: data object lifecycle
     *                 should be own by event handler)
     *
     */

    @Override
    public void onNewOrderSingle(NewOrderSingle newOrder) {
    }

    @Override
    public void onOrderCancelReplaceRequest(OrderCancelReplaceRequest replaceRequest) {

    }

    @Override
    public void onOrderCancelRequest(OrderCancelRequest cancelRequest) {

    }

    @Override
    public void onOrderCancelReject() {

    }

    @Override
    public void onBusinessReject() {

    }
}
