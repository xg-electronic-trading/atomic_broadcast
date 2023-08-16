package atomic_broadcast.client;

import atomic_broadcast.utils.InstanceInfo;
import atomic_broadcast.utils.JournalState;
import atomic_broadcast.utils.TransportParams;
import atomic_broadcast.utils.TransportState;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import static atomic_broadcast.utils.JournalState.ActiveJournal;
import static atomic_broadcast.utils.TransportState.*;

public class ClientTransportWorker implements TransportWorker {

    private final Log log = LogFactory.getLog(ClientTransportWorker.class.getName());

    private final InstanceInfo instanceInfo;
    private final TransportParams params;
    private final EventSubscriber transportClient;
    private TransportState state = NoState;

    public ClientTransportWorker(TransportParams params,
                                 EventSubscriber transportClient,
                                 InstanceInfo instanceInfo) {
        this.params = params;
        this.transportClient = transportClient;
        this.instanceInfo = instanceInfo;
    }

    @Override
    public void start() {
        setState(ConnectToJournalSource);
    }

    @Override
    public void close() {
        try {
            transportClient.close();
            setState(Stopped);
        } catch (Exception e) {
            log.error().append("error whilst closing: ").appendLast(e);
        }
    }

    @Override
    public void poll() {
        doWork();
    }

    @Override
    public TransportState state() {
        return state;
    }

    private int doWork() {
        switch (state) {
            case NoState:
            case Stopped:
                break;
            case ConnectToJournalSource:
                connectToJournalSource();
                break;
            case FindJournal:
                findJournal();
                break;
            case StartReplayMerge:
                startReplayMerge();
                break;
            case PollReplayMerge:
                pollReplay();
                break;
            case PollEventStream:
                pollEventStream();
                break;
        }

        return state.getCode();
    }

    private void connectToJournalSource() {
        if (transportClient.connectToJournalSource()) {
            setState(FindJournal);
        } else {
            setState(ConnectToJournalSource);
        }
    }

    private void findJournal() {
        JournalState journalState = transportClient.findJournal();
        if (journalState == ActiveJournal) {
            setState(StartReplayMerge);
        }
    }

    private void startReplayMerge() {
        if (transportClient.connectToEventStream()) {
            setState(PollReplayMerge);
        }
    }

    private void pollReplay() {
        if (transportClient.pollReplay()) {
            setState(PollEventStream);
        }
    }

    private void pollEventStream() {
        if (transportClient.isSubscriptionClosed()) {
            setState(StartReplayMerge);
        } else {
            transportClient.pollEventStream();
        }
    }

    private void  setState(TransportState newState) {
        if (this.state != newState) {
            state = newState;
            log.info().append("app: ").append(instanceInfo.app())
                    .append(", instance: ").append(instanceInfo.instance())
                    .append(", new state: ").appendLast(state);
        }
    }
}
