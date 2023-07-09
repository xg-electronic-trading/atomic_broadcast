package atomic_broadcast.client;

import atomic_broadcast.utils.InstanceInfo;
import atomic_broadcast.utils.TransportParams;
import atomic_broadcast.utils.TransportState;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import static atomic_broadcast.utils.TransportState.*;

public class ClientPublicationWorker implements TransportWorker {

    private final Log log = LogFactory.getLog(ClientPublicationWorker.class.getName());

    private final InstanceInfo instanceInfo;
    private final TransportParams params;
    private final CommandPublisher commandPublisher;
    private TransportState state = NoState;

    public ClientPublicationWorker(TransportParams params,
                                   CommandPublisher commandPublisher,
                                   InstanceInfo instanceInfo) {
        this.params = params;
        this.commandPublisher = commandPublisher;
        this.instanceInfo = instanceInfo;
    }

    @Override
    public void  start() {
        setState(ConnectToCommandStream);
    }

    @Override
    public void close() {
        try {
            commandPublisher.close();
            setState(NoState);
        } catch (Exception e) {
            log.error().append("error whilst closing: ").appendLast(e);
        }
    }

    @Override
    public void poll() {
        doWork();
    }

    private int doWork() {
        switch (state) {
            case ConnectToCommandStream:
                connectToCommandStream();
                break;
            case ConnectedToCommandStream:
                monitorConnection();
                break;

        }
        return state.getCode();
    }

    private void connectToCommandStream() {
        if (commandPublisher.connectToCommandStream()) {
            setState(ConnectedToCommandStream);
        }
    }

    private void monitorConnection() {
        if (commandPublisher.isPublicationClosed()) {
            setState(ConnectToCommandStream);
        }
    }

    @Override
    public TransportState state() {
        return state;
    }

    private void setState(TransportState newState) {
        if (this.state != newState) {
            state = newState;
            log.info().append("app: ").append(instanceInfo.app())
                    .append(", instance: ").append(instanceInfo.instance())
                    .append(", new state: ").appendLast(state);
        }
    }
}
