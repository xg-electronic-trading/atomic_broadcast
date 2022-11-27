package atomic_broadcast.sequencer;

import atomic_broadcast.client.TransportSession;
import atomic_broadcast.consensus.SeqNumSnapshot;
import atomic_broadcast.consensus.ShmSeqNoClient;
import atomic_broadcast.utils.TransportParams;
import atomic_broadcast.utils.TransportState;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import org.agrona.concurrent.UnsafeBuffer;

import static atomic_broadcast.utils.TransportState.*;

public class SequencerTransportWorker implements TransportSession {

    private static final Log log = LogFactory.getLog(SequencerTransportWorker.class.getName());

    private final TransportParams params;
    private final SequencerTransport transportClient;
    private final ShmSeqNoClient shmSeqNoClient;

    private TransportState state = NoState;

    public SequencerTransportWorker(
            TransportParams params,
            SequencerTransport transportClient,
            ShmSeqNoClient shmSeqNoClient) {
        this.params = params;
        this.transportClient = transportClient;
        this.shmSeqNoClient = shmSeqNoClient;
    }


    @Override
    public boolean isSubscriptionConnected() {
        return false;
    }

    @Override
    public boolean isPublicationConnected() {
        return false;
    }

    @Override
    public void start() {
        state = FindLeader;
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean poll() {
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
            case StartReplication:
                break;
            case StopRepliaction:
                break;
            case ConnectToEventStream:
                state = transportClient.connectToEventStream() ? PollEventStream : ConnectToEventStream;
                break;
            case PollEventStream:
                transportClient.pollEventStream();
                break;
        }

        return true;
    }

    private void determineLeader() {
        SeqNumSnapshot snapshot = shmSeqNoClient.readSeqNums();
        if(snapshot.isReady()) {
            if (params.instanceId() == snapshot.leaderInstance()) {
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
        SeqNumSnapshot snapshot = shmSeqNoClient.readSeqNums();
        if (snapshot.isReady()) {
            boolean journalFound = transportClient.findJournal();
            boolean isLeader = snapshot.leaderInstance() == params.instanceId();
            if (!journalFound && isLeader) {
                setState(CreateNewJournal);
            } else {
                setState(ConnectToEventStream);
            }
        }
    }

    private void setState(TransportState newState) {
        if (this.state != newState) {
            state = newState;
            log.info().append("new state: ").appendLast(state);
        }
    }

    @Override
    public boolean publish(UnsafeBuffer buffer, int offset, int length) {
        return false;
    }
}
