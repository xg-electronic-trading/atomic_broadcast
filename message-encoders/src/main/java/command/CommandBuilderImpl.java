package command;

import com.messages.sbe.OrdTypeEnum;
import com.messages.sbe.SideEnum;
import com.messages.sbe.StrategyEnum;
import com.messages.sbe.TimeInForceEnum;
import schema.api.PacketWriter;

public class CommandBuilderImpl implements CommandBuilder {

    private final PacketWriter packet = new PacketWriter();
    private final NewOrderSingleCommand newOrderSingle = new NewOrderSingleCommandImpl(packet);

    @Override
    public NewOrderSingleCommand createNewOrderSingle() {
        newOrderSingle.beginWrite();
        newOrderSingle
                .parentId(0)
                .symbol("")
                .side(SideEnum.Buy)
                .transactTime(0)
                .qty(0)
                .tif(TimeInForceEnum.Day)
                .ordType(OrdTypeEnum.Limit)
                .price(0)
                .strategy(StrategyEnum.NULL_VAL)
                .exDest(0)
                .startTime(0)
                .endTime(0);

        return newOrderSingle;
    }
}
