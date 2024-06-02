package atomic_broadcast.utils;

import atomic_broadcast.listener.MessageListener;
import reusable.Resettable;

import java.util.ArrayList;
import java.util.List;

public class TransportParams implements Resettable {

    private ConnectAs connectAs;
    private ConnectUsing connectUsing;
    private EventReaderType eventReaderType;
    private String publicationChannel;
    private int instanceId = -1;
    private int electionTimeoutSecs = -1;
    private final ArrayList<MessageListener> listeners = new ArrayList<>(10);

    public TransportParams() {
        reset();
    }

    public TransportParams connectAs(ConnectAs connectAs) {
        this.connectAs = connectAs;
        return this;
    }

    public TransportParams connectUsing(ConnectUsing connectUsing) {
        this.connectUsing = connectUsing;
        return this;
    }

    public TransportParams withEventReader(EventReaderType eventReaderType) {
        this.eventReaderType = eventReaderType;
        return this;
    }

    public TransportParams instance(int instanceId) {
        this.instanceId = instanceId;
        return this;
    }

    public TransportParams addListener(MessageListener listener) {
        listeners.add(listener);
        return this;
    }

    public TransportParams addPublicationChannel(String channel) {
        this.publicationChannel = channel;
        return this;
    }

    public TransportParams electionTimeoutSecs(int seconds) {
        this.electionTimeoutSecs = seconds;
        return this;
    }


    public ConnectAs connectAs() {
        return connectAs;
    }

    public ConnectUsing connectUsing() {
        return connectUsing;
    }

    public EventReaderType eventReaderType() { return eventReaderType; }

    public List<MessageListener> listeners() { return listeners; }

    public int instanceId() { return instanceId; }

    public String publicationChannel() { return publicationChannel; }

    public int electionTimeoutSecs() { return electionTimeoutSecs; }

    @Override
    public void reset() {
        connectAs = ConnectAs.Client;
        connectUsing = ConnectUsing.Ipc;
        eventReaderType = EventReaderType.Direct;
        publicationChannel = "";
        instanceId = -1;
        electionTimeoutSecs = -1;
        for (int i = listeners.size() - 1; i > -1; i--) {
            listeners.remove(i);
        }
    }
}
