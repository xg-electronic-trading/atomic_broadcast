package atomic_broadcast.consensus;

public interface SeqNumSnapshotWriter {
    void setReady(boolean isReady);
    void writeSeqNum(int component, int instance, int seqNo);
}
