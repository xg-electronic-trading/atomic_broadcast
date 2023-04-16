package command;

import com.messages.sbe.*;
import org.agrona.DirectBuffer;
import schema.api.PacketWriter;

public class CommandBuilderImpl implements CommandBuilder {

    private final PacketWriter packet = new PacketWriter();

    //consensus commands
    private final AppendEntriesCommandimpl appendEntries = new AppendEntriesCommandimpl(packet);
    private final RequestVoteCommandImpl requestVote = new RequestVoteCommandImpl(packet);
    private final RequestVoteResponseCommandImpl requestVoteResponse = new RequestVoteResponseCommandImpl(packet);

    //order commands
    private final NewOrderSingleCommandImpl newOrderSingle = new NewOrderSingleCommandImpl(packet);


    @Override
    public AppendEntriesCommandimpl createAppendEntries() {
        appendEntries.beginWrite();
        appendEntries.term(0).leaderId(0);
        return appendEntries;
    }

    @Override
    public RequestVoteCommandImpl createRequestVote() {
        requestVote.beginWrite();
        requestVote.term(0)
                .candidateId(0)
                .seqNo(0)
                .logPosition(0);

        return requestVote;
    }

    @Override
    public RequestVoteResponseCommandImpl createRequestVoteResponse() {
        requestVoteResponse.beginWrite();
        requestVoteResponse.term(0)
                .instanceid(0)
                .voteGranted(false);

        return requestVoteResponse;
    }

    @Override
    public NewOrderSingleCommandImpl createNewOrderSingle() {
        newOrderSingle.beginWrite();
        newOrderSingle
                .parentId(0)
                .symbol("")
                .side(SideEnum.Buy)
                .transactTime(0)
                .qty(0)
                .tif(TimeInForceEnum.Day)
                .ordType(OrdTypeEnum.Limit)
                .price(0)
                .strategy(StrategyEnum.NULL_VAL)
                .exDest(0)
                .startTime(0)
                .endTime(0);

        return newOrderSingle;
    }

    @Override
    public DirectBuffer buffer() {
        return packet.buffer();
    }


}
