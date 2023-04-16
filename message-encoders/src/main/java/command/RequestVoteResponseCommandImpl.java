package command;

import com.messages.sbe.BooleanType;
import com.messages.sbe.RequestVoteResponseEncoder;
import schema.api.CommandImpl;
import schema.api.PacketWriter;

public class RequestVoteResponseCommandImpl extends CommandImpl implements RequestVoteResponseCommand {

    private final RequestVoteResponseEncoder encoder = new RequestVoteResponseEncoder();

    public RequestVoteResponseCommandImpl(PacketWriter packet) {
        super(packet);
        setEncoder(encoder);
    }


    @Override
    public RequestVoteResponseCommand term(long term) {
        encoder.term(term);
        return this;
    }

    @Override
    public RequestVoteResponseCommand instanceid(long instance) {
        encoder.instanceId(instance);
        return this;
    }

    @Override
    public RequestVoteResponseCommand voteGranted(boolean voteGranted) {
        encoder.voteGranted(voteGranted ? BooleanType.T : BooleanType.F);
        return this;
    }
}
