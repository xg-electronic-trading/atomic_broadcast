package atomic_broadcast.aeron;

import atomic_broadcast.listener.MessageListener;
import atomic_broadcast.sequencer.SequencerClient;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import schema.api.PacketReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class AeronSequencerFragmentHandler implements FragmentHandler {

    private final List<MessageListener> listeners;
    private final SequencerClient sequencerClient;
    private final AtomicLong seqNoHolder = new AtomicLong(-1);

    PacketReader packet = new PacketReader();

    public AeronSequencerFragmentHandler(SequencerClient sequencerClient, List<MessageListener> listeners) {
        this.sequencerClient = sequencerClient;
        this.listeners = listeners;
    }

    @Override
    public void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        long seqNo = seqNoHolder.incrementAndGet();
        packet.wrap(buffer, offset);
        packet.encodeSeqNo(seqNo);
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onMessage(packet, false);
        }

        sequencerClient.publish(packet.buffer(), offset, length);
    }

    public long seqNo() {
        return seqNoHolder.get();
    }
}
