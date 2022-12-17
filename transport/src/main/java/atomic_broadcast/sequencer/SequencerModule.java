package atomic_broadcast.sequencer;

import atomic_broadcast.client.TransportSession;
import atomic_broadcast.consensus.SeqNoProvider;
import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.TransportParams;
import atomic_broadcast.utils.TransportState;

public class SequencerModule implements Module {

    private final TransportParams params;
    private final SequencerClient transport;
    private final SeqNoProvider seqNoProvider;
    private TransportSession transportSession;

    public SequencerModule(
            TransportParams params,
            SequencerClient transport,
            SeqNoProvider seqNoProvider) {
        this.params = params;
        this.transport = transport;
        this.seqNoProvider = seqNoProvider;
    }

    @Override
    public void start() {
        switch (params.connectAs()) {
            case Sequencer:
                this.transportSession = new SequencerTransportWorker(params, transport, seqNoProvider);

        }

        transportSession.start();
    }

    @Override
    public void close() {
        transportSession.stop();
    }

    @Override
    public void poll() {
        transportSession.poll();
    }

    public TransportState state() { return transportSession.state(); }
}
