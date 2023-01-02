package schema.api;

public class MessageType {
    public static final short NewOrderSingle = 0;
    public static final short OrderCancelReplaceRequest = 1;
    public static final short OrderCancelRequest = 2;
    public static final short ExecutionReport = 3;
    public static final short OrderCancelReject = 4;
    public static final short BusinessMessageReject = 5;
}
