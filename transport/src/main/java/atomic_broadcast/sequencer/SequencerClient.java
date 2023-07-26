package atomic_broadcast.sequencer;

import atomic_broadcast.client.EventSubscriber;
import org.agrona.DirectBuffer;

public interface SequencerClient extends EventSubscriber {

    boolean connectToCommandStream();

    boolean pollCommandStream();

    boolean startReplication();

    boolean stopReplication();

    boolean createEventStream();

    boolean extendEventStream();

    boolean createEventJournal();

    boolean extendEventJournal();

    boolean isPublicationConnected();

    boolean isPublicationClosed();

    boolean publish(DirectBuffer buffer, int offset, int length);

    long position();
}
