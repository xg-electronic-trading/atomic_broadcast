package algo;

import com.messages.sbe.StrategyEnum;
import immutable.ImmutableList;
import orderstate.OrderState;
import schema.api.NewOrderSingle;

import java.util.List;

public interface AlgoCode {

    StrategyEnum name();

    ImmutableList<AlgoAction> onPendingNew(NewOrderSingle newOrder, OrderState orderState, AlgoContext ctx);

    ImmutableList<AlgoAction> onOrderAccepted(OrderState orderState, AlgoContext ctx);

    ImmutableList<AlgoAction> onPendingReplace();

    ImmutableList<AlgoAction> onReplaceAccepted();

    ImmutableList<AlgoAction> onPendingCancel();

    ImmutableList<AlgoAction> onCancelAccepted();

    ImmutableList<AlgoAction> onTick();
}
