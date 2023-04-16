package command;

import org.agrona.DirectBuffer;

public interface CommandBuilder {

    AppendEntriesCommandimpl createAppendEntries();

    RequestVoteCommandImpl createRequestVote();

    RequestVoteResponseCommandImpl createRequestVoteResponse();

    NewOrderSingleCommandImpl createNewOrderSingle();

    DirectBuffer buffer();
}
