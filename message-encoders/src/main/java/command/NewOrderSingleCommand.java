package command;

import com.messages.sbe.OrdTypeEnum;
import com.messages.sbe.SideEnum;
import com.messages.sbe.StrategyEnum;
import com.messages.sbe.TimeInForceEnum;

public interface NewOrderSingleCommand {

    NewOrderSingleCommand parentId(long id);

    NewOrderSingleCommand price(long price);

    NewOrderSingleCommand qty(int qty);

    NewOrderSingleCommand side(SideEnum side);

    NewOrderSingleCommand symbol(CharSequence sym);

    NewOrderSingleCommand ordType(OrdTypeEnum ordType);

    NewOrderSingleCommand tif(TimeInForceEnum tif);

    NewOrderSingleCommand strategy(StrategyEnum strategy);

    NewOrderSingleCommand exDest(int exDest);

    NewOrderSingleCommand startTime(long startTime);

    NewOrderSingleCommand endTime(long endTime);

    NewOrderSingleCommand transactTime(long time);
}
