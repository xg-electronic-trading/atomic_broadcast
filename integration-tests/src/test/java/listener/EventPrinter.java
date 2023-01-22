package listener;

import atomic_broadcast.listener.MessageListener;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.messages.sbe.NewOrderSingleDecoder;
import org.agrona.collections.LongHashSet;
import schema.api.Packet;

import static schema.api.MessageType.NewOrderSingle;

public class EventPrinter implements MessageListener {

    private static final Log log = LogFactory.getLog(EventPrinter.class.getName());

    private final LongHashSet ids = new LongHashSet(100);
    NewOrderSingleDecoder nosDecoder = new NewOrderSingleDecoder();

    @Override
    public void onMessage(Packet packet) {
        switch (packet.messageType()) {
            case NewOrderSingle:
                nosDecoder.wrap(packet.buffer(), packet.offset(), packet.fixedMessageLength(), packet.version());
                log.info().appendLast(nosDecoder.toString());
                ids.add(nosDecoder.id());
                break;
            default:
                System.out.println("cannot print unkown message type: " + packet.messageType());
        }
    }

    public boolean isCommandAcked(long id) {
        return ids.contains(id);
    }
}
