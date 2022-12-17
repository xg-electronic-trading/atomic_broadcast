package atomic_broadcast.sequencer;

import atomic_broadcast.client.TransportClient;

public interface SequencerClient extends TransportClient {

    boolean connectToCommandStream();

    boolean pollCommandStream();

    boolean startReplication();

    boolean stopReplication();

    boolean createEventStream();

    boolean createEventJournal();

    boolean isPublicationConnected();

}
