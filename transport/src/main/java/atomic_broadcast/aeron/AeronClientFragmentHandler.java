package atomic_broadcast.aeron;

import atomic_broadcast.listener.MessageListener;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import schema.api.Packet;
import schema.api.PacketReader;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class AeronClientFragmentHandler implements FragmentHandler {

    private final List<MessageListener> listeners;
    private final AtomicLong seqNoHolder = new AtomicLong(-1);
    private final PacketReader packet = new PacketReader();

    public AeronClientFragmentHandler(List<MessageListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        packet.wrap(buffer, offset, length);
        long incomingSeqNo = packet.seqNo();
        long currentSeqNo = seqNoHolder.get();

        if (incomingSeqNo <= currentSeqNo) {
            /**
             * handle duplicate messages
             */
        } else if (incomingSeqNo == currentSeqNo + 1) {
            seqNoHolder.incrementAndGet();
            /**
             *  1. iterate through each listener
             *  2. check if seq no. less than highwatermark to determine if is a replay message.
             *  3. update highwatermark
             */
            packet.encodeIsReplay(0);
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).onMessage(packet);
            }
        } else {
            /**
             * handle missed messages via replay.
             */
        }
    }
}
