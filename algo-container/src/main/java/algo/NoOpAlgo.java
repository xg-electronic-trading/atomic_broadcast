package algo;

import com.messages.sbe.StrategyEnum;
import immutable.ImmutableList;
import immutable.ImmutableListImpl;
import orderstate.OrderState;
import schema.api.NewOrderSingle;

import java.util.ArrayList;
import java.util.List;

public class NoOpAlgo implements AlgoCode {

    private final List<AlgoAction> actions = new ArrayList<>(20);
    private final ImmutableListImpl<AlgoAction> immutableList = new ImmutableListImpl<>();

    @Override
    public StrategyEnum name() {
        return null;
    }

    @Override
    public ImmutableList<AlgoAction> onPendingNew(NewOrderSingle newOrder, OrderState orderState, AlgoContext ctx) {
        return immutableList.set(actions);
    }

    @Override
    public ImmutableList<AlgoAction> onOrderAccepted(OrderState orderState, AlgoContext ctx) {
        return immutableList.set(actions);
    }

    @Override
    public ImmutableList<AlgoAction> onPendingReplace() {
        return immutableList.set(actions);
    }

    @Override
    public ImmutableList<AlgoAction> onReplaceAccepted() {
        return immutableList.set(actions);
    }

    @Override
    public ImmutableList<AlgoAction> onPendingCancel() {
        return immutableList.set(actions);
    }

    @Override
    public ImmutableList<AlgoAction> onCancelAccepted() {
        return immutableList.set(actions);
    }

    @Override
    public ImmutableList<AlgoAction> onTick() {
        return immutableList.set(actions);
    }
}
