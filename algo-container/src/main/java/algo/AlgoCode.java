package algo;

import com.messages.sbe.StrategyEnum;
import orderstate.OrderState;
import schema.api.NewOrderSingle;

public interface AlgoCode {

    StrategyEnum name();

    void onPendingNew(NewOrderSingle newOrder, OrderState orderState, AlgoContext ctx);

    void onPendingReplace();

    void onPendingCancel();

    void onEvent();
}
