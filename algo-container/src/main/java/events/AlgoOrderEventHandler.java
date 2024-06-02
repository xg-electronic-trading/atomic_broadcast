package events;

import algo.AlgoAction;
import algo.AlgoCode;
import algo.AlgoContext;
import algo.AlgoFactory;
import immutable.ImmutableList;
import orderstate.ByteBufferOrderStateCache;
import orderstate.OrderStateFlyweight;
import org.agrona.collections.Long2ObjectHashMap;
import schema.api.NewOrderSingle;
import schema.api.OrderCancelReplaceRequest;
import schema.api.OrderCancelRequest;

import java.util.List;

public class AlgoOrderEventHandler implements OrderEventHandler {

    private final ByteBufferOrderStateCache osCache;
    private final AlgoContext algoContext;
    private final AlgoFactory algoFactory;
    private final Long2ObjectHashMap<AlgoCode> algoInstanceMap = new Long2ObjectHashMap<>(5_000, 0.65f, true);

    public AlgoOrderEventHandler(ByteBufferOrderStateCache osCache,
                                 AlgoContext algoContext,
                                 AlgoFactory algoFactory) {
        this.osCache = osCache;
        this.algoContext = algoContext;
        this.algoFactory = algoFactory;

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
        OrderStateFlyweight flyweight = osCache.orderState(newOrder.id());
        flyweight.setPrice(newOrder.price());
        flyweight.setQuantity(newOrder.qty());

        /**
         * create algo instance per parent order.
         * algo code should be created by a factory that is owned by
         * strategy repo/module
         */
        AlgoCode algoCode = algoFactory.createAlgo(newOrder.strategy());
        ImmutableList<AlgoAction> actions = algoCode.onPendingNew(newOrder, flyweight, algoContext);
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
