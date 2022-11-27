package atomic_broadcast.consensus;

public class SeqNumSnapshotWriter implements SeqNumSnapshot {

    private boolean isReady;

    public void setIsReady(boolean isReady) {
        this.isReady = isReady;
    }

    @Override
    public boolean isReady() {
        return isReady;
    }

    @Override
    public long instanceSeqNum(int instanceId) {
        return 0;
    }

    @Override
    public int leaderInstance() {
        return 0;
    }
}
