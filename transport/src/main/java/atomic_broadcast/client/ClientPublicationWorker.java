package atomic_broadcast.client;

import atomic_broadcast.utils.TransportParams;
import atomic_broadcast.utils.TransportState;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import static atomic_broadcast.utils.TransportState.*;

public class ClientPublicationWorker implements TransportWorker {

    private static final Log log = LogFactory.getLog(ClientPublicationWorker.class.getName());

    private final TransportParams params;
    private final CommandPublisher commandPublisher;
    private TransportState state = NoState;

    public ClientPublicationWorker(TransportParams params, CommandPublisher commandPublisher) {
        this.params = params;
        this.commandPublisher = commandPublisher;
    }

    @Override
    public void  start() {
        setState(ConnectToCommandStream);
    }

    @Override
    public void close() {
        try {
            commandPublisher.close();
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

    private void  setState(TransportState newState) {
        if (this.state != newState) {
            state = newState;
            log.info().append("new state: ").appendLast(state);
        }
    }
}
