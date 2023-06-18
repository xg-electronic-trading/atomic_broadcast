package algo;

import com.messages.sbe.StrategyEnum;
import orderstate.OrderState;
import schema.api.NewOrderSingle;

public class AlgoCodeImpl implements AlgoCode {


    @Override
    public StrategyEnum name() {
        return null;
    }

    @Override
    public void onPendingNew(NewOrderSingle newOrder, OrderState orderState, AlgoContext ctx) {

    }

    @Override
    public void onPendingReplace() {

    }

    @Override
    public void onPendingCancel() {

    }

    @Override
    public void onEvent() {

    }
}
