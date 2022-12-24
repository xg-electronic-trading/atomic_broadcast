package atomic_broadcast.aeron;

import atomic_broadcast.listener.MessageListener;
import atomic_broadcast.sequencer.SequencerClient;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class AeronSequencerFragmentHandler implements FragmentHandler {

    private final List<MessageListener> listeners;
    private final SequencerClient sequencerClient;
    private final AtomicLong seqNoHolder = new AtomicLong(-1);

    //UnsafeBuffer sendBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(1024));

    public AeronSequencerFragmentHandler(SequencerClient sequencerClient, List<MessageListener> listeners) {
        this.sequencerClient = sequencerClient;
        this.listeners = listeners;
    }

    @Override
    public void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        //sendBuffer.wrap(buffer);
        long seqNo = seqNoHolder.incrementAndGet();
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onMessage(buffer, offset, length, seqNo, false);
        }

        //sequencerClient.publish(sendBuffer, offset, length);
    }

    public long seqNo() {
        return seqNoHolder.get();
    }
}
