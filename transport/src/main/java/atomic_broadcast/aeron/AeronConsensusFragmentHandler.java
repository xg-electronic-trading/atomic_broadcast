package atomic_broadcast.aeron;

import atomic_broadcast.listener.MessageListener;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import schema.api.PacketReader;

import java.util.List;

public class AeronConsensusFragmentHandler implements FragmentHandler {
    private final PacketReader packet = new PacketReader();

    private final List<MessageListener> listeners;

    public AeronConsensusFragmentHandler(List<MessageListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        packet.wrap(buffer, offset, length);
        for (int l = 0; l < listeners.size(); l++) {
            listeners.get(l).onMessage(packet);
        }
    }
}
