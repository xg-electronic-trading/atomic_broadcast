package atomic_broadcast.consensus;

public interface SeqNumSnapshot {

    boolean isReady();

    long instanceSeqNum(int instanceId);

    int leaderInstance();
}
