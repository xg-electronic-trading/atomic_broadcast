package atomic_broadcast.consensus;

import org.agrona.collections.Long2LongHashMap;

public class SeqNumSnapshotReader implements SeqNumSnapshot {

    private boolean isReady;
    private final Long2LongHashMap map = new Long2LongHashMap(20, 0.55f, -1, true);
    private int leader = -1;
    private long currentMaxSeqNo = -1;

    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    public void setInstanceSeqNum(int instanceId, long seqNo) {
        if(seqNo > currentMaxSeqNo) {
            currentMaxSeqNo = seqNo;
            leader = instanceId;
        }
        map.put(instanceId, seqNo);
    }

    @Override
    public boolean isReady() {
        return isReady;
    }

    @Override
    public long instanceSeqNum(int instanceId) {
        return map.get(instanceId);
    }

    @Override
    public int leaderInstance() {
        return leader;
    }
}
