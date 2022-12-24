package atomic_broadcast.sequencer;

import org.agrona.concurrent.UnsafeBuffer;

public class SequencerEventPublisher implements EventPublisher {

    private final SequencerClient sequencerClient;

    public SequencerEventPublisher(SequencerClient sequencerClient) {
        this.sequencerClient = sequencerClient;
    }

    @Override
    public boolean publish(UnsafeBuffer buffer, int offset, int length) {
        return sequencerClient.publish(buffer, offset, length);
    }
}
