package atomic_broadcast.client;

import atomic_broadcast.utils.TransportParams;
import atomic_broadcast.utils.TransportState;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import static atomic_broadcast.utils.TransportState.*;

public class ClientTransportWorker implements TransportWorker {

    private static final Log log = LogFactory.getLog(ClientTransportWorker.class.getName());

    private final TransportParams params;
    private final TransportClient transportClient;
    private TransportState state = NoState;

    public ClientTransportWorker(TransportParams params, TransportClient transportClient) {
        this.params = params;
        this.transportClient = transportClient;
    }

    @Override
    public void start() {
        state = ConnectToJournalSource;
    }

    @Override
    public void close() {
        try {
            transportClient.close();
        } catch (Exception e) {
            log.error().append("error whilst closing: ").appendLast(e);
        }
    }

    @Override
    public boolean poll() {
        doWork();
        return true;
    }

    @Override
    public TransportState state() {
        return state;
    }

    private int doWork() {
        switch (state) {
            case NoState:
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
        if (transportClient.findJournal()) {
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

    private void setState(TransportState newState) {
        if (this.state != newState) {
            state = newState;
            log.info().append("new state: ").appendLast(state);
        }
    }
}
