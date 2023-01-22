package atomic_broadcast.listener;

import atomic_broadcast.utils.Pollable;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import schema.api.PacketReader;

public class RingBufferEventsReader implements Pollable {

    private final RingBuffer ringBuffer;
    private final MessageHandler msgHandler;

    public RingBufferEventsReader(MessageListener listener, RingBuffer ringBuffer) {
        this.msgHandler = new MessageHandlerImpl(listener);
        this.ringBuffer = ringBuffer;
    }


    public void poll() {
        int messagesRead = ringBuffer.read(msgHandler, 500);
    }


    private class MessageHandlerImpl implements MessageHandler {

        private final PacketReader packet = new PacketReader();
        private final MessageListener listener;

        MessageHandlerImpl(MessageListener listener) {
            this.listener = listener;
        }

        @Override
        public void onMessage(int msgId, MutableDirectBuffer mutableDirectBuffer, int offset, int length) {
            packet.wrap(mutableDirectBuffer, offset, length);
            listener.onMessage(packet);
        }
    }



}
