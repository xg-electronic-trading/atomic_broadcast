package atomic_broadcast.consensus;

public class ConsensusStateSnapshotReader implements ConsensusStateSnapshot {

    private int instance;
    private long seqNo = -1;
    private long logPosition = -1;
    private long currentTerm = -1;
    private long votedFor = -1;

    public void setSequencerConsensusState(int instance,
                                  long seqNo,
                                  long logPosition,
                                  long currentTerm,
                                  long votedFor) {
        this.instance = instance;
        this.seqNo = seqNo;
        this.logPosition = logPosition;
        this.currentTerm = currentTerm;
        this.votedFor = votedFor;
    }

    @Override
    public long seqNo() {
        return seqNo;
    }

    @Override
    public int instance() {
        return instance;
    }

    @Override
    public long logPosition() {
        return logPosition;
    }

    @Override
    public long currentTerm() {
        return currentTerm;
    }

    @Override
    public long votedFor() {
        return votedFor;
    }
}
