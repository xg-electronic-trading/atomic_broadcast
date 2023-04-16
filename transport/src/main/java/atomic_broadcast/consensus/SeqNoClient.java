package atomic_broadcast.consensus;

public class SeqNoClient implements ClientSeqNumWriter {

    private final ShmSeqNoClient shmClient;

    public SeqNoClient(ShmSeqNoClient shmClient) {
        this.shmClient = shmClient;
    }

    @Override
    public void writeSeqNum(int instance, long seqNo) {
        shmClient.writeSeqNum(instance, seqNo);
    }
}
