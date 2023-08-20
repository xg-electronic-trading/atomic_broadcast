package atomic_broadcast.aeron;

import atomic_broadcast.consensus.ConsensusStateSnapshot;
import atomic_broadcast.consensus.SeqNoClient;
import atomic_broadcast.listener.MessageListener;
import atomic_broadcast.sequencer.SequencerClient;
import atomic_broadcast.utils.InstanceInfo;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import io.aeron.Aeron;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import schema.api.PacketReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class AeronSequencerFragmentHandler implements FragmentHandler {

    private static final Log log = LogFactory.getLog(AeronSequencerClient.class.getName());

    private final InstanceInfo instanceInfo;
    private final int instance;
    private final List<MessageListener> listeners;
    private final SequencerClient sequencerClient;
    private final SeqNoClient seqNoClient;
    private final AtomicLong seqNoHolder = new AtomicLong(-1);

    PacketReader packet = new PacketReader();

    public AeronSequencerFragmentHandler(
            InstanceInfo instanceInfo,
            SequencerClient sequencerClient,
            List<MessageListener> listeners,
            SeqNoClient seqNoClient,
            int instance) {
        this.sequencerClient = sequencerClient;
        this.listeners = listeners;
        this.seqNoClient = seqNoClient;
        this.instance = instance;
        this.instanceInfo = instanceInfo;
    }

    /**
     * unsequenced commands (seqNo = -1) are:
     *      1) sequenced
     *      2) run through any business logic
     *      3) published onto event stream
     *      4) seq no highwatermark persisted to mmap file.
     *
     * sequenced commands (seqNo > -1) are:
     *      1) treated as replay messages
     *      2) set latest SeqNo.
     *      3) run through any business logic to repopulate state
     */

    @Override
    public void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        packet.wrap(buffer, offset, length);

        if (packet.seqNo() == Aeron.NULL_VALUE) {
            long seqNo = seqNoHolder.incrementAndGet();
            packet.encodeSeqNo(seqNo);
            invokeMessageListeners();

            sequencerClient.publish(packet.buffer(), 0, length);
            seqNoClient.writeSeqNum(instance, seqNo, sequencerClient.position());
        } else {
            seqNoHolder.set(packet.seqNo());
            invokeMessageListeners();
            seqNoClient.writeSeqNum(instance, seqNoHolder.get(), header.position());
        }
    }

    private void invokeMessageListeners() {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onMessage(packet);
        }
    }

    public long seqNo() {
        return seqNoHolder.get();
    }
}
