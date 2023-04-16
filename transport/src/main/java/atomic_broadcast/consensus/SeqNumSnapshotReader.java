package atomic_broadcast.consensus;

public class SeqNumSnapshotReader implements SeqNumSnapshot {

    private int instance;
    private long seqNo = -1;

    public void setInstanceSeqNum(int instance, long seqNo) {
        this.instance = instance;
        this.seqNo = seqNo;
    }

    @Override
    public long seqNo() {
        return seqNo;
    }

    @Override
    public int instance() {
        return instance;
    }
}
