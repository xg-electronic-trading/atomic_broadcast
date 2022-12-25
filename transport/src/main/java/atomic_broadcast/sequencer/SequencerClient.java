package atomic_broadcast.sequencer;

import atomic_broadcast.client.TransportClient;
import org.agrona.DirectBuffer;

public interface SequencerClient extends TransportClient {

    boolean connectToCommandStream();

    boolean pollCommandStream();

    boolean startReplication();

    boolean stopReplication();

    boolean createEventStream();

    boolean createEventJournal();

    boolean isPublicationConnected();

    boolean isPublicationClosed();

    boolean publish(DirectBuffer buffer, int offset, int length);

}
