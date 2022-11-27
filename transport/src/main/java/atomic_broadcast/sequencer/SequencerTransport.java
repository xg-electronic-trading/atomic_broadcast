package atomic_broadcast.sequencer;

import atomic_broadcast.client.TransportClient;

public interface SequencerTransport extends TransportClient {

    boolean connectToCommandStream();

    boolean pollCommandStream();

}
