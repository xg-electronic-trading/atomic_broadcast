package atomic_broadcast.consensus;

import atomic_broadcast.utils.InstanceInfo;
import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.ModuleName;
import atomic_broadcast.utils.Pollable;

public class ConsensusModule implements Module {

    private final ConsensusWorker worker;
    private final ConsensusStateHolder consensusState;
    private final InstanceInfo instanceInfo;

    private boolean started = false;

    @Override
    public ModuleName name() {
        return ModuleName.Consensus;
    }

    @Override
    public InstanceInfo instanceInfo() {
        return instanceInfo;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    public ConsensusModule(ConsensusTransportClient transportClient,
                           ConsensusStateHolder consensusStateHolder,
                           InstanceInfo instanceInfo) {
        this.consensusState = consensusStateHolder;
        this.instanceInfo = instanceInfo;
        worker = new ConsensusWorker(transportClient, consensusStateHolder, instanceInfo);
    }

    public Pollable transport() {
        return worker;
    }

    @Override
    public void start() {
        worker.start();
        started = true;
    }

    @Override
    public void close() {
        worker.close();
        started = false;
    }

    public ConsensusStateHolder consensusState() {
        return consensusState;
    }
}
