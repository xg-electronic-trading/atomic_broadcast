package events;

import atomic_broadcast.listener.MessageListener;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import schema.api.Packet;

public class RingBufferEventsWriter implements MessageListener {

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
        ringBuffer.write(0, packet.buffer(), 0, packet.buffer().capacity());
    }
}
