package events;

import schema.api.NewOrderSingle;
import schema.api.OrderCancelReplaceRequest;
import schema.api.OrderCancelRequest;

public class AlgoOrderEventHandler implements OrderEventHandler {


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
