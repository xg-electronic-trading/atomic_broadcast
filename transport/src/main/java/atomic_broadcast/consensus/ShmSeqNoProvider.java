package atomic_broadcast.consensus;

public class ShmSeqNoProvider implements SeqNoProvider {

    private final ShmSeqNoClient shmClient;

    public ShmSeqNoProvider(ShmSeqNoClient shmClient) {
        this.shmClient = shmClient;
    }

    @Override
    public SeqNumSnapshot takeSnapshot() {
        return shmClient.readSeqNums();
    }
}
