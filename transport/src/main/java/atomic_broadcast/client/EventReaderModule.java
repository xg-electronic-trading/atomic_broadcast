package atomic_broadcast.client;

import atomic_broadcast.listener.MessageListener;
import atomic_broadcast.listener.RingBufferEventsReader;
import atomic_broadcast.listener.RingBufferEventsWriter;
import atomic_broadcast.utils.*;
import atomic_broadcast.utils.Module;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;

import java.nio.ByteBuffer;

import static atomic_broadcast.utils.ModuleName.ClientTransport;

public class EventReaderModule implements Module {

    private final InstanceInfo instanceInfo;
    private final TransportParams params;
    private final MessageListener listener;
    private final TransportWorker transportSession;
    private RingBufferEventsReader eventsFromRingBufferReader;
    private EventReaderType eventReaderType;

    private boolean started;


    public EventReaderModule(EventSubscriber transportClient,
                             TransportParams params,
                             MessageListener listener,
                             InstanceInfo instanceInfo) {
        this.params = params;
        this.listener = listener;
        this.instanceInfo = instanceInfo;
        this.eventReaderType = params.eventReaderType();

        switch (params.connectAs()) {
            case Client:
                if (params.eventReaderType() == EventReaderType.Direct) {
                    params.addListener(listener);
                }

                if (params.eventReaderType() == EventReaderType.RingBuffer) {
                    UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect((2 * 1024 * 1024) + RingBufferDescriptor.TRAILER_LENGTH));
                    RingBuffer ringBuffer = new OneToOneRingBuffer(buffer);
                    RingBufferEventsWriter eventsToRingBufferWriter = new RingBufferEventsWriter(ringBuffer);
                    eventsFromRingBufferReader = new RingBufferEventsReader(listener, ringBuffer);
                    params.addListener(eventsToRingBufferWriter);
                }

                transportSession = new ClientTransportWorker(params, transportClient, instanceInfo);
                break;
            default:
                throw new IllegalArgumentException("error: trying to connect as: " + params.connectAs());
        }
    }

    @Override
    public ModuleName name() {
        return ClientTransport;
    }

    @Override
    public InstanceInfo instanceInfo() {
        return instanceInfo;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void start() {
        transportSession.start();
        started = true;
    }

    @Override
    public void close() {
        transportSession.close();
        started = false;
    }

    public Pollable transport() {
        return transportSession;
    }

    public MessageListener listener() {
        return listener;
    }

    public Pollable eventsReader() {
        if (params.eventReaderType() == EventReaderType.Direct) {
            return transportSession;
        } else {
            return eventsFromRingBufferReader;
        }
    }

    public EventReaderType eventReaderType() { return eventReaderType; }

    public TransportState state() { return transportSession.state(); }

    public TransportParams params() { return params; }
}
