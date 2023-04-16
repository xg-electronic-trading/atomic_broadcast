package schema.api.consensus;

public interface RequestVoteResponse {
    long instanceId(); //instance from which reponse is received
    long term();
    boolean voteGranted();
}
