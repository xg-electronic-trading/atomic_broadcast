package atomic_broadcast.consensus;

public interface ClientSeqNumReader {

    ConsensusStateSnapshot readSeqNum();
}
