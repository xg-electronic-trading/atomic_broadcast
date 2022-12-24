package atomic_broadcast.sequencer;

import atomic_broadcast.listener.MessageListener;
import org.agrona.DirectBuffer;

public class SequencerCommandHandler implements MessageListener {

    @Override
    public void onMessage(DirectBuffer buffer, int offset, int length, long seqNum, boolean isReplay) {

    }
}
