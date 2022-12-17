package atomic_broadcast.aeron;

import atomic_broadcast.listener.MessageListener;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class AeronSequencerFragmentHandler implements FragmentHandler {

    private final List<MessageListener> listeners;
    private final AtomicLong seqNoHolder = new AtomicLong(-1);

    public AeronSequencerFragmentHandler(List<MessageListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        long seqNo = seqNoHolder.incrementAndGet();
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onMessage(buffer, offset, length, seqNo, false);
        }
    }

    public long seqNo() {
        return seqNoHolder.get();
    }
}
