package atomic_broadcast.consensus;

public interface ConsensusTransportClient extends AutoCloseable {
    void initialise();
    boolean pollSubscription();
    boolean hasHeartbeatTimeoutExpired();
    boolean startElection();
    boolean sendHeartbeat();
}
