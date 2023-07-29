package atomic_broadcast.sequencer;

import atomic_broadcast.client.TransportWorker;
import atomic_broadcast.utils.InstanceInfo;
import atomic_broadcast.utils.JournalState;
import atomic_broadcast.utils.ReplayState;
import atomic_broadcast.utils.TransportState;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import static atomic_broadcast.utils.JournalState.JournalNotFound;
import static atomic_broadcast.utils.TransportState.*;

public class LeaderTransportWorker implements TransportWorker {

    private final Log log = LogFactory.getLog(LeaderTransportWorker.class.getName());

    private final InstanceInfo instanceInfo;
    private final SequencerClient transportClient;

    private TransportState state = NoState;

    public LeaderTransportWorker(
            InstanceInfo instanceInfo,
            SequencerClient transportClient) {
        this.instanceInfo = instanceInfo;
        this.transportClient = transportClient;
    }

    @Override
    public void start() {
        setState(FindJournal);
    }

    @Override
    public void close() {
        setState(Stopped);
    }

    @Override
    public TransportState state() {
        return state;
    }

    @Override
    public void poll() {
        switch (state) {
            case NoState:
            case Stopped:
                break;
            case FindJournal:
                findJournal();
                break;
            case CreateEventStream:
                createEventStream();
                break;
            case ExtendEventStream:
                extendEventStream();
                break;
            case CreateEventJournal:
                createNewJournal();
                break;
            case ExtendEventJournal:
                extendEventJournal();
                break;
            case ConnectToCommandStream:
                connectToCommandStream();
                break;
            case PollCommandStream:
                pollCommandStream();
                break;
            case StartReplay:
                startReplay();
                break;
            case CheckReplayActive:
                checkReplayActive();
                break;
            case PollReplay:
                pollReplay();
                break;
        }
    }

    private void findJournal() {
        JournalState journalState = transportClient.findJournal();
        if (journalState == JournalNotFound) {
            setState(CreateEventStream);
        } else {
            setState(StartReplay);
        }
    }

    private void startReplay() {
        ReplayState replayState = transportClient.startReplay();
        switch (replayState) {
            case NotStarted:
                break;
            case Started:
                setState(CheckReplayActive);
                break;
            case Skipped:
                setState(ExtendEventStream);
                break;
        }
    }

    private void checkReplayActive() {
        if (transportClient.isReplayActive()) {
            setState(PollReplay);
        }
    }

    private void pollReplay() {
        boolean isDone = transportClient.pollReplay();
        if (isDone) {
            setState(ExtendEventStream);
        }
    }

    private void createEventStream() {
        boolean eventStreamCreated = transportClient.createEventStream();
        if (eventStreamCreated) {
            setState(CreateEventJournal);
        }
    }

    private void extendEventStream() {
        boolean eventStreamExtended = transportClient.extendEventStream();
        if (eventStreamExtended) {
            setState(ExtendEventJournal);
        }
    }

    private void createNewJournal() {
        boolean isJournalCreated = transportClient.createEventJournal();
        if (isJournalCreated) {
            setState(ConnectToCommandStream);
        }
    }

    private void extendEventJournal() {
        boolean isJournalExtended = transportClient.extendEventJournal();
        if (isJournalExtended) {
            setState(ConnectToCommandStream);
        }
    }

    private void connectToCommandStream() {
        boolean isSubscriptionCreated = transportClient.connectToCommandStream();
        if (isSubscriptionCreated) {
            setState(PollCommandStream);
        }
    }

    private void pollCommandStream() {
        if (transportClient.isPublicationClosed()) {
            setState(ConnectToCommandStream);
        } else {
            transportClient.pollCommandStream();
        }
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
