package schema.api;

import com.messages.sbe.*;

public class OrderCancelReplaceRequestImpl implements OrderCancelReplaceRequest {

    private final OrderCancelReplaceRequestDecoder decoder = new OrderCancelReplaceRequestDecoder();

    public void init(Packet packet) {
        decoder.wrap(
                packet.buffer(),
                packet.offset(),
                packet.fixedMessageLength(),
                packet.version()
        );
    }

    @Override
    public long parentId() {
        return 0;
    }

    @Override
    public long price() {
        return 0;
    }

    @Override
    public long qty() {
        return 0;
    }

    @Override
    public SideEnum side() {
        return null;
    }

    @Override
    public CharSequence symbol() {
        return null;
    }

    @Override
    public OrdTypeEnum ordType() {
        return null;
    }

    @Override
    public TimeInForceEnum tif() {
        return null;
    }

    @Override
    public StrategyEnum strategy() {
        return null;
    }

    @Override
    public int exDest() {
        return 0;
    }

    @Override
    public long startTime() {
        return 0;
    }

    @Override
    public long endTime() {
        return 0;
    }

    @Override
    public long id() {
        return 0;
    }
}
