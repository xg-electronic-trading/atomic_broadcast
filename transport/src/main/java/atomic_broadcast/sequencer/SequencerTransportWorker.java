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
    private boolean active = false;

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
        } catch (Exception e){
            log.error().append("error whilst closing: ").appendLast(e);
        }
    }

    @Override
    public void poll() {
        switch (state) {
            case NoState:
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
            case CreateEventJournal:
                createNewJournal();
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
            case StartReplayMerge:
                state = transportClient.connectToEventStream() ? PollEventStream : StartReplayMerge;
                break;
            case StartReplay:
                break;
            case PollEventStream:
                transportClient.pollEventStream();
                break;
        }
    }

    @Override
    public TransportState state() {
        return state;
    }

    private void determineLeader() {
        if(consensusStateHolder.isLeaderAssigned()) {
            if (consensusStateHolder.isLeader()) {
                active = true;
                setState(ConnectToJournalSource);
            } else {
                setState(StartReplication);
            }
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
        if (!journalFound) {
            if (active) {
                setState(CreateEventStream);
            } else {
                setState(StartReplication);
            }
        } else {
            if (active) {
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

    private void createNewJournal() {
        boolean isJournalCreated = transportClient.createEventJournal();
        if (isJournalCreated) {
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
            setState(StartReplayMerge);
        }
    }

    private void stopReplication() {
        boolean isReplicationStopped = transportClient.stopReplication();
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
