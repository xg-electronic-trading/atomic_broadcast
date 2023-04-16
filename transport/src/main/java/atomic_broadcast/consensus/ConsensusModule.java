package atomic_broadcast.consensus;

import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.ModuleName;
import atomic_broadcast.utils.Pollable;

public class ConsensusModule implements Module {

    private final ConsensusWorker worker;
    private final ConsensusStateHolder consensusState;

    @Override
    public ModuleName name() {
        return ModuleName.Consensus;
    }

    public ConsensusModule(ConsensusTransportClient transportClient, ConsensusStateHolder consensusStateHolder) {
        this.consensusState = consensusStateHolder;
        worker = new ConsensusWorker(transportClient, consensusStateHolder);
    }

    public Pollable transport() {
        return worker;
    }

    @Override
    public void start() {
        worker.start();
    }

    @Override
    public void close() {
        worker.close();
    }

    public ConsensusStateHolder consensusState() {
        return consensusState;
    }
}
