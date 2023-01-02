package events;

import schema.api.NewOrderSingle;
import schema.api.OrderCancelReplaceRequest;
import schema.api.OrderCancelRequest;

public interface OrderEventHandler {

    void onNewOrderSingle(NewOrderSingle newOrder);
    void onOrderCancelReplaceRequest(OrderCancelReplaceRequest replaceRequest);
    void onOrderCancelRequest(OrderCancelRequest cancelRequest);
    void onOrderCancelReject();
    void onBusinessReject();

}
