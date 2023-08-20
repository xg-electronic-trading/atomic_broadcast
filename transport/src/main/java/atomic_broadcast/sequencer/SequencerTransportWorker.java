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
    private final LeaderTransportWorker leaderWorker;
    private final FollowerTransportWorker followerWorker;

    private String lastExceptionMessage = "";

    private TransportState state = NoState;
    private TransportState innerWorkerState = NoState;
    private boolean shouldPoll = false;

    public SequencerTransportWorker(
            TransportParams params,
            SequencerClient transportClient,
            ConsensusStateHolder consensusStateHolder,
            InstanceInfo instanceInfo) {
        this.params = params;
        this.transportClient = transportClient;
        this.consensusStateHolder = consensusStateHolder;
        this.instanceInfo = instanceInfo;
        this.leaderWorker = new LeaderTransportWorker(instanceInfo, transportClient);
        this.followerWorker = new FollowerTransportWorker(instanceInfo, transportClient);
    }

    @Override
    public void start() {
        setState(FindLeader);
        leaderWorker.start();
        followerWorker.start();
        shouldPoll = true;
    }

    @Override
    public void close() {
        try {
            transportClient.close();
            leaderWorker.close();
            followerWorker.close();
            setState(Stopped);
            innerWorkerState = Stopped;
            shouldPoll = false;
        } catch (Exception e){
            log.error().append("error whilst closing: ").appendLast(e);
        }
    }

    @Override
    public void poll() {
        try {
            if (shouldPoll) {
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
                    case PollLeader:
                        pollLeader();
                        break;
                    case PollFollower:
                        pollFollower();
                        break;
                }
            }
        } catch (Exception e) {
            if (!lastExceptionMessage.equals(e.getMessage())) {
                log.error().append("app: ").append(instanceInfo.app())
                        .append(", instance: ").append(instanceInfo.instance())
                        .append(", state: ").append(state)
                        .append(", exception ").appendLast(e.getMessage());

                lastExceptionMessage = e.getMessage();
                shouldPoll = false;
            }
        }
    }

    @Override
    public TransportState state() {
        return innerWorkerState;
    }

    private void determineLeader() {
        if(consensusStateHolder.isLeaderAssigned()) {
            setState(ConnectToJournalSource);
        }
    }

    private void connectToJournalSource() {
        if (transportClient.connectToJournalSource()) {
            if (consensusStateHolder.isLeader()) {
                setState(PollLeader);
            } else {
                setState(PollFollower);
            }
        } else {
            setState(ConnectToJournalSource);
        }
    }

    private void pollLeader() {
        if (consensusStateHolder.isLeader()) {
            leaderWorker.poll();
            innerWorkerState = leaderWorker.state();
        } else {
            /**
             * close leader worker if leader has been demoted to follower
             */
            leaderWorker.close();
            setState(PollFollower);
        }
    }

    private void pollFollower() {
        if (consensusStateHolder.isLeader()) {
            /**
             * close follower worker if been promoted to leader.
             */
            followerWorker.close();
            setState(PollLeader);
        } else {
            followerWorker.poll();
            innerWorkerState = followerWorker.state();
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
