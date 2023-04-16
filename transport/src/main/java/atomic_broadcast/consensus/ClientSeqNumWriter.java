package atomic_broadcast.consensus;

public interface ClientSeqNumWriter {
    void writeSeqNum(int instance, long seqNo);
}
