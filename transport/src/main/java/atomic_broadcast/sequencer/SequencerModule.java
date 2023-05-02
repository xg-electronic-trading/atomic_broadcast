package atomic_broadcast.sequencer;

import atomic_broadcast.client.TransportWorker;
import atomic_broadcast.consensus.ConsensusStateHolder;
import atomic_broadcast.utils.*;
import atomic_broadcast.utils.Module;

import static atomic_broadcast.utils.ModuleName.Sequencer;

public class SequencerModule implements Module {
    private final TransportWorker transportSession;

    public SequencerModule(
            TransportParams params,
            SequencerClient transport,
            ConsensusStateHolder consensusStateHolder,
            InstanceInfo instanceInfo) {
        switch (params.connectAs()) {
            case Sequencer:
                this.transportSession = new SequencerTransportWorker(params, transport, consensusStateHolder, instanceInfo);
                break;
            default:
                throw new IllegalArgumentException("error: trying to connect as: " + params.connectAs());

        }
    }

    @Override
    public ModuleName name() {
        return Sequencer;
    }

    @Override
    public void start() {
        transportSession.start();
    }

    @Override
    public void close() {
        transportSession.close();
    }


    public Pollable transport() {
        return transportSession;
    }

    public TransportState state() { return transportSession.state(); }
}
