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
    public void writeSeqNum(int component, int instance, int seqNo) {
        shmClient.writeSeqNum(component, instance, seqNo);
    }
}
