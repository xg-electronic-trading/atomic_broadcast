package schema.api;

import com.messages.sbe.OrdTypeEnum;
import com.messages.sbe.SideEnum;
import com.messages.sbe.StrategyEnum;
import com.messages.sbe.TimeInForceEnum;

public interface OrderCancelReplaceRequest extends OrderEvent {
    long parentId();

    long price();

    long qty();

    SideEnum side();

    CharSequence symbol();

    OrdTypeEnum ordType();

    TimeInForceEnum tif();

    StrategyEnum strategy();

    int exDest();

    long startTime();

    long endTime();
}
