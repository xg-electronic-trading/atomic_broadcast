package atomic_broadcast.consensus;

public interface ClientConsensusStateWriter {
    void writeConsensusState(long term, long votedFor);
}
