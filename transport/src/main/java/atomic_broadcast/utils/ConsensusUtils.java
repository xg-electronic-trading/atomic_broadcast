package atomic_broadcast.utils;

public class ConsensusUtils {

    public static long quorumThreshold(long clusterMembers) {
        return (clusterMembers >> 1) + 1;
    }
}
