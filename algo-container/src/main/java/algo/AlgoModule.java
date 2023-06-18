package algo;

import atomic_broadcast.client.CommandProcessor;
import atomic_broadcast.listener.MessageListener;
import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.ModuleName;
import command.CommandBuilder;
import events.AlgoEventListener;
import events.AlgoOrderEventHandler;
import events.OrderEventHandler;
import orderstate.ByteBufferOrderStateCache;
import orderstate.OrderStateField;

import static atomic_broadcast.utils.ModuleName.Algo;

public class AlgoModule implements Module {

    private final MessageListener eventListener;

    public AlgoModule(CommandProcessor cmdProcessor, CommandBuilder cmdBuilder) {
        OrderStateField[] fields = new OrderStateField[]{
                OrderStateField.Id,
                OrderStateField.Price,
                OrderStateField.Quantity,
                OrderStateField.MsgSeqNum};
        ByteBufferOrderStateCache cache = new ByteBufferOrderStateCache(false, 1, fields);
        AlgoContext ctx = new AlgoContextImpl(cmdProcessor, cmdBuilder);
        OrderEventHandler eventHandler = new AlgoOrderEventHandler(cache, ctx);
        this.eventListener = new AlgoEventListener(eventHandler);
    }

    @Override
    public ModuleName name() {
        return Algo;
    }

    @Override
    public void start() {

    }

    @Override
    public void close() {

    }
}
