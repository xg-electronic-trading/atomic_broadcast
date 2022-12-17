package atomic_broadcast.consensus;

public interface ClientSeqNumWriter {
    void writeSeqNum(int component, int instance, int seqNo);
}
