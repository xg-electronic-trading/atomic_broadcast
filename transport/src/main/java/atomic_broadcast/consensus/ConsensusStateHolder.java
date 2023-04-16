package atomic_broadcast.consensus;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import static atomic_broadcast.consensus.ClusterTransportState.*;

public class ConsensusStateHolder {

    private static final Log log = LogFactory.getLog(ConsensusStateHolder.class.getName());

    private ClusterTransportState state;
    private String leaderHostname = "localhost";
    private boolean requestedVote;
    private int leaderInstance = -1;

    public void setLeaderHostname(String leaderHostname) { this.leaderHostname = leaderHostname; }

    public void setLeaderInstance(int instanceId) { this.leaderInstance = instanceId; }

    public void setRequestedVote(boolean requestedVote) {
        this.requestedVote = requestedVote;
    }

    public void setState(ClusterTransportState newState) {
        if (this.state != newState) {
            state = newState;
            log.info().append("new state: ").appendLast(state);
        }
    }

    public String getLeaderHostname() {
        return leaderHostname;
    }

    public long getLeaderInstance() {
        return leaderInstance;
    }

    public ClusterTransportState getState() {
        return state;
    }

    public boolean hasRequestedVote() {
        return requestedVote;
    }

    public boolean isFollower() {
        return state == Follower;
    }

    public boolean isLeader() {
        return state == Leader;
    }

    public boolean isCandidate() {
        return state == Candidate;
    }

    public boolean isLeaderAssigned() { return leaderInstance != -1; }
}
