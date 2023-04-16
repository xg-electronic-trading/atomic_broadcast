package schema.api.consensus;

public interface RequestVote {
    long term();
    long candidateId();
    long seqNo();
    long logPosition();
}
