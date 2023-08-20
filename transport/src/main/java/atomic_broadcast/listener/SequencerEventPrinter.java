package atomic_broadcast.listener;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.messages.sbe.NewOrderSingleDecoder;
import schema.api.Packet;

import static schema.api.MessageType.NewOrderSingle;

public class SequencerEventPrinter implements MessageListener {

    private static final Log log = LogFactory.getLog(SequencerEventPrinter.class.getName());

    private final NewOrderSingleDecoder nosDecoder = new NewOrderSingleDecoder();

    @Override
    public void onMessage(Packet packet) {
        switch (packet.messageType()) {
            case NewOrderSingle:
                nosDecoder.wrap(packet.buffer(), packet.offset(), packet.fixedMessageLength(), packet.version());
                if (log.isDebugEnabled()) {
                    log.debug()
                            .append("seqNo: ").append(packet.seqNo()).append(", ")
                            .appendLast(nosDecoder.toString());
                }
                break;
            default:
                System.out.println("cannot print unkown message type: " + packet.messageType());
        }
    }
}
