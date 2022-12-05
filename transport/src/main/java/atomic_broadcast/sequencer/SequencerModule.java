package atomic_broadcast.sequencer;

import atomic_broadcast.consensus.SeqNoProvider;
import atomic_broadcast.consensus.ShmSeqNoClient;
import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.TransportParams;
import atomic_broadcast.utils.TransportState;

public class SequencerModule implements Module {

    private final TransportParams params;
    private final SequencerTransport transport;
    private final SeqNoProvider seqNoProvider;
    private TransportState state = TransportState.NoState;

    public SequencerModule(
            TransportParams params,
            SequencerTransport transport,
            SeqNoProvider seqNoProvider) {
        this.params = params;
        this.transport = transport;
        this.seqNoProvider = seqNoProvider;
    }

    @Override
    public void start() {
        switch (params.connectAs()) {
            case Sequencer:
                new SequencerTransportWorker(params, transport, seqNoProvider);

        }
    }

    @Override
    public void close() {

    }

    @Override
    public void poll() {

    }
}
