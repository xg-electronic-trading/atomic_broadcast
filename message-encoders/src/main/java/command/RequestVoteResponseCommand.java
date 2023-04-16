package command;

public interface RequestVoteResponseCommand {
    RequestVoteResponseCommand term(long term);
    RequestVoteResponseCommand instanceid(long instance);
    RequestVoteResponseCommand voteGranted(boolean voteGranted);
}
