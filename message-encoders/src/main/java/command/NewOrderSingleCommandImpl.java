package command;

import com.messages.sbe.*;
import schema.api.PacketWriter;

public class NewOrderSingleCommandImpl implements NewOrderSingleCommand, Command {

    private final NewOrderSingleEncoder encoder = new NewOrderSingleEncoder();
    private final PacketWriter packet;

    public NewOrderSingleCommandImpl(PacketWriter packet) {
        this.packet = packet;
    }

    @Override
    public NewOrderSingleCommand parentId(long id) {
        encoder.parentId(id);
        return this;
    }

    @Override
    public NewOrderSingleCommand price(long price) {
        encoder.price().mantissa(price);
        return this;
    }

    @Override
    public NewOrderSingleCommand qty(int qty) {
        encoder.orderQty().mantissa(qty);
        return this;
    }

    @Override
    public NewOrderSingleCommand side(SideEnum side) {
        encoder.side(side);
        return this;
    }

    @Override
    public NewOrderSingleCommand symbol(CharSequence sym) {
        encoder.symbol(sym);
        return this;
    }

    @Override
    public NewOrderSingleCommand ordType(OrdTypeEnum ordType) {
        encoder.ordType(ordType);
        return this;
    }

    @Override
    public NewOrderSingleCommand tif(TimeInForceEnum tif) {
        encoder.timeInForce(tif);
        return this;
    }

    @Override
    public NewOrderSingleCommand strategy(StrategyEnum strategy) {
        encoder.strategy(strategy);
        return this;
    }

    @Override
    public NewOrderSingleCommand exDest(int exDest) {
        encoder.exDest(exDest);
        return this;
    }

    @Override
    public NewOrderSingleCommand startTime(long startTime) {
        encoder.effectiveTime().time(startTime);
        return this;
    }

    @Override
    public NewOrderSingleCommand endTime(long endTime) {
        encoder.expireTime().time(endTime);
        return this;
    }

    @Override
    public NewOrderSingleCommand transactTime(long time) {
        encoder.transactTime().time(time);
        return this;
    }

    @Override
    public void beginWrite() {
        int headerLength = packet.encodeHeader(encoder);
        encoder.wrap(packet.buffer(), headerLength);
    }

    @Override
    public void endWrite() {
        packet.reset(encoder.encodedLength());
    }
}
