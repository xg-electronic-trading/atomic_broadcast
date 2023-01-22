package events;

import atomic_broadcast.listener.MessageListener;
import schema.api.NewOrderSingleImpl;
import schema.api.OrderCancelReplaceRequestImpl;
import schema.api.OrderCancelRequestImpl;
import schema.api.Packet;

import static schema.api.MessageType.*;

public class AlgoEventListener implements MessageListener {

    private final OrderEventHandler orderEventHandler;

    private final NewOrderSingleImpl newOrder = new NewOrderSingleImpl();
    private final OrderCancelReplaceRequestImpl replaceOrder = new OrderCancelReplaceRequestImpl();
    private final OrderCancelRequestImpl cancelOrder = new OrderCancelRequestImpl();

    public AlgoEventListener(OrderEventHandler orderEventHandler) {
        this.orderEventHandler = orderEventHandler;
    }

    @Override
    public void onMessage(Packet packet) {
        switch (packet.messageType()) {
            case NewOrderSingle:
                newOrder.init(packet);
                orderEventHandler.onNewOrderSingle(newOrder);
                break;
            case OrderCancelReplaceRequest:
                replaceOrder.init(packet);
                orderEventHandler.onOrderCancelReplaceRequest(replaceOrder);
                break;
            case OrderCancelRequest:
                cancelOrder.init(packet);
                orderEventHandler.onOrderCancelRequest(cancelOrder);
                break;
            case OrderCancelReject:
                orderEventHandler.onOrderCancelReject();
                break;
            case BusinessMessageReject:
                orderEventHandler.onBusinessReject();
                break;
            case ExecutionReport:
                break;
            default:

        }
    }
}
