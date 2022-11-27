package atomic_broadcast.sequencer;

import atomic_broadcast.consensus.ShmSeqNoClient;
import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.TransportParams;
import atomic_broadcast.utils.TransportState;

public class SequencerModule implements Module {

    private final TransportParams params;
    private final SequencerTransport transport;
    private final ShmSeqNoClient shmSeqNoClient;
    private TransportState state = TransportState.NoState;

    public SequencerModule(
            TransportParams params,
            SequencerTransport transport,
            ShmSeqNoClient shmSeqNoClient) {
        this.params = params;
        this.transport = transport;
        this.shmSeqNoClient = shmSeqNoClient;
    }

    @Override
    public void start() {
        switch (params.connectAs()) {
            case Sequencer:
                new SequencerTransportWorker(params, transport, shmSeqNoClient);

        }
    }

    @Override
    public void close() {

    }

    @Override
    public void poll() {

    }
}
