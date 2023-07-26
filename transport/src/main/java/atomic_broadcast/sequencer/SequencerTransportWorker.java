package atomic_broadcast.sequencer;

import atomic_broadcast.client.TransportWorker;
import atomic_broadcast.consensus.ConsensusStateHolder;
import atomic_broadcast.utils.InstanceInfo;
import atomic_broadcast.utils.TransportParams;
import atomic_broadcast.utils.TransportState;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import static atomic_broadcast.utils.TransportState.*;

public class SequencerTransportWorker implements TransportWorker {

    private final Log log = LogFactory.getLog(SequencerTransportWorker.class.getName());

    private final InstanceInfo instanceInfo;
    private final ConsensusStateHolder consensusStateHolder;
    private final TransportParams params;
    private final SequencerClient transportClient;

    private String lastExceptionMessage = "";

    private TransportState state = NoState;

    public SequencerTransportWorker(
            TransportParams params,
            SequencerClient transportClient,
            ConsensusStateHolder consensusStateHolder,
            InstanceInfo instanceInfo) {
        this.params = params;
        this.transportClient = transportClient;
        this.consensusStateHolder = consensusStateHolder;
        this.instanceInfo = instanceInfo;
    }

    @Override
    public void start() {
        setState(FindLeader);
    }

    @Override
    public void close() {
        try {
            transportClient.close();
            setState(Stopped);
        } catch (Exception e){
            log.error().append("error whilst closing: ").appendLast(e);
        }
    }

    @Override
    public void poll() {
        try {
            switch (state) {
                case NoState:
                case Stopped:
                    break;
                case FindLeader:
                    determineLeader();
                    break;
                case ConnectToJournalSource:
                    connectToJournalSource();
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
                case StartReplication:
                    startReplication();
                    break;
                case StopRepliaction:
                    stopReplication();
                    break;
                case StartReplay:
                    startReplay();
                    break;
                case PollReplay:
                    pollReplay();
                    break;
            }
        } catch (Exception e) {
            if (!lastExceptionMessage.equals(e.getMessage())) {
                log.error().append("app: ").append(instanceInfo.app())
                        .append(", instance: ").append(instanceInfo.instance())
                        .append(", state: ").append(state)
                        .append(", exception").appendLast(e.getMessage());

                lastExceptionMessage = e.getMessage();
            }
        }
    }

    @Override
    public TransportState state() {
        return state;
    }

    private void determineLeader() {
        if(consensusStateHolder.isLeaderAssigned()) {
            setState(ConnectToJournalSource);
        }
    }

    private void connectToJournalSource() {
        if (transportClient.connectToJournalSource()) {
            setState(FindJournal);
        } else {
            setState(ConnectToJournalSource);
        }
    }

    private void findJournal() {
        boolean journalFound = transportClient.findJournal();
        boolean isLeader = consensusStateHolder.isLeader();
        if (!journalFound) {
            if (isLeader) {
                setState(CreateEventStream);
            } else {
                setState(StartReplication);
            }
        } else {
            if (isLeader) {
                setState(StartReplay);
            } else {
                setState(StartReplication);
            }
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

    private void startReplication() {
        boolean replicationStarted = transportClient.startReplication();
        if (replicationStarted) {
            setState(StartReplay);
        }
    }

    private void stopReplication() {
        boolean isReplicationStopped = transportClient.stopReplication();
    }

    private void startReplay() {
        if (transportClient.connectToEventStream()) {
            setState(PollReplay);
        }
    }

    private void pollReplay() {
        if (consensusStateHolder.isLeader()) {
            boolean isDone = transportClient.pollReplay();
            if (isDone) {
                setState(ExtendEventStream);
            }
        } else {
            transportClient.pollReplay();
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
