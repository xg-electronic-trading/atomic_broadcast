package atomic_broadcast.consensus;

public class SeqNoClient implements SeqNoProvider, ClientSeqNumWriter {

    private final ShmSeqNoClient shmClient;

    public SeqNoClient(ShmSeqNoClient shmClient) {
        this.shmClient = shmClient;
    }

    @Override
    public SeqNumSnapshot takeSnapshot() {
        return shmClient.readSeqNums();
    }

    @Override
    public void writeSeqNum(boolean isReady, int instance, long seqNo) {
        shmClient.writeSeqNum(isReady, instance, seqNo);
    }
}
