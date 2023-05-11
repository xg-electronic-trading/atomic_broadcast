package atomic_broadcast.consensus;

public class SeqNoClient implements ClientSeqNumWriter, ClientSeqNumReader, ClientConsensusStateWriter {

    private final ShmSeqNoClient shmClient;

    public SeqNoClient(ShmSeqNoClient shmClient) {
        this.shmClient = shmClient;
    }

    @Override
    public void writeSeqNum(int instance, long seqNo, long logPosition) {
        shmClient.writeSeqNum(instance, seqNo, logPosition);
    }

    @Override
    public ConsensusStateSnapshot readSeqNum() {
        return shmClient.readSeqNums();
    }

    @Override
    public void writeConsensusState(long term, long votedFor) {
        shmClient.writeConsensusState(term, votedFor);
    }
}
