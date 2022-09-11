package atomic_broadcast.sequencer;

import atomic_broadcast.client.TransportSession;
import org.agrona.concurrent.Agent;

public class SequencerTransport implements TransportSession, Agent {
    @Override
    public boolean isSubscriptionConnected() {
        return false;
    }

    @Override
    public boolean isPublicationConnected() {
        return false;
    }

    @Override
    public int doWork() throws Exception {
        return 0;
    }

    @Override
    public String roleName() {
        return null;
    }
}
