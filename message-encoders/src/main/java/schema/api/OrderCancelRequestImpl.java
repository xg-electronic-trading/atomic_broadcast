package schema.api;

import com.messages.sbe.OrderCancelRequestDecoder;

public class OrderCancelRequestImpl implements OrderCancelRequest {

    private final OrderCancelRequestDecoder decoder = new OrderCancelRequestDecoder();

    public void init(Packet packet) {
        decoder.wrap(
                packet.buffer(),
                packet.offset(),
                packet.fixedMessageLength(),
                packet.version()
        );
    }

    @Override
    public long id() {
        return 0;
    }
}
