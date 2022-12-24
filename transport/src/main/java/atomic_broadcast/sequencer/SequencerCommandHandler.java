package atomic_broadcast.sequencer;

import atomic_broadcast.listener.MessageListener;
import org.agrona.DirectBuffer;

public class SequencerCommandHandler implements MessageListener {

    private final EventPublisher eventPublisher;

    public SequencerCommandHandler(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onMessage(DirectBuffer buffer, int offset, int length, long seqNum, boolean isReplay) {

    }
}
