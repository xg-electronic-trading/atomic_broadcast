package atomic_broadcast.consensus;

import atomic_broadcast.client.TransportWorker;
import atomic_broadcast.utils.InstanceInfo;
import atomic_broadcast.utils.TransportState;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import static atomic_broadcast.consensus.ClusterTransportState.*;

public class ConsensusWorker implements TransportWorker {

    private final Log log = LogFactory.getLog(this.getClass().getName());

    private final ConsensusTransportClient consensusTransport;
    private final ConsensusStateHolder consensusStateHolder;
    private final InstanceInfo instanceInfo;
    private boolean requestedVote = false;

    public ConsensusWorker(ConsensusTransportClient consensusTransport,
                           ConsensusStateHolder consensusStateHolder,
                           InstanceInfo instanceInfo) {
        this.consensusTransport = consensusTransport;
        this.consensusStateHolder = consensusStateHolder;
        this.instanceInfo = instanceInfo;
    }

    @Override
    public void start() {
        consensusTransport.initialise();
        consensusStateHolder.setRequestedVote(false);
        consensusStateHolder.setState(Follower);
    }

    @Override
    public void close() {
        try {
            consensusTransport.close();
            consensusStateHolder.setState(Stopped);
            consensusStateHolder.resetLeaderInstance();
        } catch (Exception e){
            log.error().append("error whilst closing: ").appendLast(e);
        }
    }

    @Override
    public TransportState state() {
        return null;
    }

    @Override
    public void poll() {
        switch (consensusStateHolder.getState()) {
            case Follower:
                onFollower();
                break;
            case Candidate:
                onCandidate();
                break;
            case Leader:
                onLeader();
                break;
            case NoState:
            case Stopped:
                break;
            default:
                throw new IllegalStateException("unknown cluster transport state: " + consensusStateHolder.getState());
        }
    }

    private void onFollower() {
        if (consensusTransport.hasHeartbeatTimeoutExpired()) {
            resetConsensusStateBeforeElection();
           consensusStateHolder.setState(Candidate);
        } else {
            consensusTransport.pollSubscription();
        }
    }

    private void onCandidate() {
        if (requestedVote) {
            if (consensusTransport.hasHeartbeatTimeoutExpired()) {
                resetConsensusStateBeforeElection();
            } else {
                consensusTransport.pollSubscription();
            }
        } else {
            requestedVote = consensusTransport.startElection();
        }
    }

    private void onLeader() {
        consensusTransport.pollSubscription();
        consensusTransport.sendHeartbeat();
    }

    private void resetConsensusStateBeforeElection() {
        requestedVote = false;
        consensusStateHolder.resetActiveClusterMembers();
    }
}
