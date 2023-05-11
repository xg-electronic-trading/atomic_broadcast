package atomic_broadcast.consensus;

public interface ConsensusStateSnapshot {

    long seqNo();

    int instance();

    long logPosition();

    long currentTerm();

    long votedFor();
}
