package schema.api;

import com.messages.sbe.*;
import string.StringConversion;

public class NewOrderSingleImpl implements NewOrderSingle {

    private final NewOrderSingleDecoder decoder = new NewOrderSingleDecoder();

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
        return decoder.parentId();
    }

    @Override
    public long price() {
        return decoder.price().mantissa();
    }

    @Override
    public long qty() {
        return decoder.orderQty().mantissa();
    }

    @Override
    public SideEnum side() {
        return decoder.side();
    }

    @Override
    public CharSequence symbol() {
        return decoder.symbol();
    }

    @Override
    public OrdTypeEnum ordType() {
        return decoder.ordType();
    }

    @Override
    public TimeInForceEnum tif() {
        return decoder.timeInForce();
    }

    @Override
    public StrategyEnum strategy() {
        return decoder.strategy();
    }

    @Override
    public int exDest() {
        return StringConversion.bufferToInt(
                decoder.buffer(),
                decoder.offset() +
                        NewOrderSingleDecoder.exDestEncodingOffset(),
                NewOrderSingleDecoder.exDestEncodingLength());
    }

    @Override
    public long startTime() {
        return decoder.effectiveTime().time();
    }

    @Override
    public long endTime() {
        return decoder.expireTime().time();
    }

    @Override
    public long id() {
        return decoder.id();
    }
}
