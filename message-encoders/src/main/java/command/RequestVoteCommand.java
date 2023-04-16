package command;

public interface RequestVoteCommand {
    RequestVoteCommand term(long term);
    RequestVoteCommand candidateId(long instance);
    RequestVoteCommand seqNo(long seqNo);
    RequestVoteCommand logPosition(long position);
}
