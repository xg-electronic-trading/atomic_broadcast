package atomic_broadcast.utils;

import atomic_broadcast.listener.MessageListener;

import java.util.ArrayList;
import java.util.List;

public class TransportParams {

    private ConnectAs connectAs = ConnectAs.Client;
    private ConnectUsing connectUsing = ConnectUsing.Ipc;
    private int instanceId;
    private final ArrayList<MessageListener> listeners = new ArrayList<>(10);

    public TransportParams connectAs(ConnectAs connectAs) {
        this.connectAs = connectAs;
        return this;
    }

    public TransportParams connectUsing(ConnectUsing connectUsing) {
        this.connectUsing = connectUsing;
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


    public ConnectAs connectAs() {
        return connectAs;
    }

    public ConnectUsing connectUsing() {
        return connectUsing;
    }

    public List<MessageListener> listeners() { return listeners; }

    public int instanceId() { return instanceId; }
}
