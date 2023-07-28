package atomic_broadcast.sequencer;

import atomic_broadcast.client.EventSubscriber;
import atomic_broadcast.utils.ReplayState;
import org.agrona.DirectBuffer;

public interface SequencerClient extends EventSubscriber {

    boolean connectToCommandStream();

    boolean pollCommandStream();

    ReplayState startReplay();

    boolean isReplayActive();

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
