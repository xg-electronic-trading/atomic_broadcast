package events;

import atomic_broadcast.AllAppsMain;
import atomic_broadcast.listener.MessageListener;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import schema.api.Packet;

public class RingBufferEventsWriter implements MessageListener {

    private static final Log log = LogFactory.getLog(RingBufferEventsWriter.class.getName());

    private final RingBuffer ringBuffer;

    /**
     * The class has a single responsibility to write events polled
     * from the event stream to a ringbuffer. The rationale here
     * is that if a process is subscribing directly to a multicast
     * stream of events it should not cause back pressure due to
     * long processing times. event processing should be handed
     * off via message passing (ring buffer) to business logic
     * thread.
     */

    public RingBufferEventsWriter(RingBuffer ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    @Override
    public void onMessage(Packet packet) {
        int offerCount = 0;
        boolean success = false;
        while (!success) {
            success = ringBuffer.write(packet.messageType(), packet.buffer(), 0, packet.buffer().capacity());
            offerCount++;
            if (!success) {
                log.warn().append("event ringbuffer blocked!!!")
                        .append(" offer count: ").append(offerCount)
                        .append(" producer position: ").append(ringBuffer.producerPosition())
                        .append(" consumer position: ").append(ringBuffer.producerPosition())
                        .append(" backlog (bytes) to consume: ").appendLast(ringBuffer.size());
            }
        }
    }
}
