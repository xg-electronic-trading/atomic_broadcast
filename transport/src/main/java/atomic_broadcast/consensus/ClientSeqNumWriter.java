package atomic_broadcast.consensus;

public interface ClientSeqNumWriter {
    void writeSeqNum(boolean isReady, int instance, long seqNo);
}
