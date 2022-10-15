package atomic_broadcast.sequencer;

import atomic_broadcast.client.TransportSession;
import atomic_broadcast.utils.SequencerTransportState;
import atomic_broadcast.utils.TransportParams;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.UnsafeBuffer;

import static atomic_broadcast.utils.SequencerTransportState.NO_STATE;

public class SequencerTransport implements TransportSession, Agent {

    private final TransportParams params;
    private SequencerTransportState state = NO_STATE;

    public SequencerTransport(TransportParams params) {
        this.params = params;
    }


    @Override
    public boolean isSubscriptionConnected() {
        return false;
    }

    @Override
    public boolean isPublicationConnected() {
        return false;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean pollSubscription() {
        return false;
    }

    @Override
    public boolean publish(UnsafeBuffer buffer, int offset, int length) {
        return false;
    }

    @Override
    public int doWork() throws Exception {
        switch (state) {
            case CONNECT_TO_ARCHIVE:
            case REPLAY:
            case ADVERTISE_SEQ_NUM:
            case CONNECT_TO_CMD_EVT_BUS:
            case TAIL_REPLICATED_RECORDING:
            default:
                throw new IllegalArgumentException("unrecognised sequencer state");
        }
    }


    private boolean connectToArchive() {
        return false;
    }

    @Override
    public String roleName() {
        return null;
    }
}
