package algo;

import atomic_broadcast.client.CommandProcessor;
import atomic_broadcast.listener.MessageListener;
import atomic_broadcast.utils.InstanceInfo;
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

    private boolean started = false;

    private final MessageListener eventListener;
    private final InstanceInfo instanceInfo;

    public AlgoModule(InstanceInfo instanceInfo,
                      CommandProcessor cmdProcessor,
                      CommandBuilder cmdBuilder,
                      AlgoFactory algoFactory) {
        this.instanceInfo = instanceInfo;
        OrderStateField[] fields = new OrderStateField[]{
                OrderStateField.Id,
                OrderStateField.Price,
                OrderStateField.Quantity,
                OrderStateField.MsgSeqNum};
        ByteBufferOrderStateCache cache = new ByteBufferOrderStateCache(false, 1, fields);
        AlgoContext ctx = new AlgoContextImpl(cmdProcessor, cmdBuilder);
        OrderEventHandler eventHandler = new AlgoOrderEventHandler(cache, ctx, algoFactory);
        this.eventListener = new AlgoEventListener(eventHandler);
    }

    public InstanceInfo instanceInfo() {
        return instanceInfo;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public ModuleName name() {
        return Algo;
    }

    @Override
    public void start() {
        started = true;
    }

    @Override
    public void close() {
        started = false;
    }
}
