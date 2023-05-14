package atomic_broadcast.consensus;

import atomic_broadcast.utils.InstanceInfo;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import static atomic_broadcast.consensus.ClusterTransportState.*;

public class ConsensusStateHolder {

    private final Log log = LogFactory.getLog(this.getClass().getName());

    private final InstanceInfo instanceInfo;
    private ClusterTransportState state;
    private String leaderHostname = "localhost";
    private boolean requestedVote;
    private long noOfActiveClusterMembers = 1;
    private int leaderInstance = -1;

    public ConsensusStateHolder(InstanceInfo instanceInfo) {
        this.instanceInfo = instanceInfo;
    }

    public void setLeaderHostname(String leaderHostname) { this.leaderHostname = leaderHostname; }

    public void setLeaderInstance(int instanceId) { this.leaderInstance = instanceId; }

    public void setRequestedVote(boolean requestedVote) {
        this.requestedVote = requestedVote;
    }

    public void setState(ClusterTransportState newState) {
        if (this.state != newState) {
            state = newState;
            log.info().append("app: ").append(instanceInfo.app())
                    .append(", instance: ").append(instanceInfo.instance())
                    .append(", new state: ").appendLast(state);
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

    public void incrementActiveClusterMembers() {
        noOfActiveClusterMembers++;
    }

    public long getNoOfActiveClusterMembers() {
        return noOfActiveClusterMembers;
    }

    public void resetActiveClusterMembers() {
        //always reset back to single member. i.e. this server.
        noOfActiveClusterMembers = 1;
    }
}
