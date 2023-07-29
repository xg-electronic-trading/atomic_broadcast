package atomic_broadcast.sequencer;

import atomic_broadcast.client.TransportWorker;
import atomic_broadcast.utils.InstanceInfo;
import atomic_broadcast.utils.JournalState;
import atomic_broadcast.utils.TransportState;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import static atomic_broadcast.utils.JournalState.ActiveJournal;
import static atomic_broadcast.utils.JournalState.InactiveJournal;
import static atomic_broadcast.utils.TransportState.*;

public class FollowerTransportWorker implements TransportWorker {

    private final Log log = LogFactory.getLog(FollowerTransportWorker.class.getName());

    private final InstanceInfo instanceInfo;
    private final SequencerClient transportClient;

    private TransportState state = NoState;

    public FollowerTransportWorker(
            InstanceInfo instanceInfo,
            SequencerClient transportClient
    ) {
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
            case StartReplication:
                startReplication();
                break;
            case StopRepliaction:
                stopReplication();
                break;
            case StartReplay:
                startReplay();
                break;
            case CheckReplayActive:
                checkReplayActive();
                break;
            case PollOpenEndedReplay:
                pollJournal();
                break;
        }

    }

    private void findJournal() {
        JournalState journalState = transportClient.findJournal();

        if (journalState == ActiveJournal) {
            /**
             * Active journal exists, therefore skip to replay
             */
            setState(StartReplay);
        } else {
            setState(StartReplication);
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
        if (transportClient.startTailEventJournal()) {
            setState(CheckReplayActive);
        }
    }

    private void checkReplayActive() {
        if (transportClient.isReplayActive()) {
            setState(PollOpenEndedReplay);
        }
    }

    private void pollJournal() {
        boolean success = transportClient.pollJournal();

        if (!success) {
            /**
             * restart replication-replay cycle
             * when disconnected from journal.
             */
            setState(FindJournal);
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
