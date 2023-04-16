package command;

import com.messages.sbe.RequestVoteEncoder;
import schema.api.CommandImpl;
import schema.api.PacketWriter;

public class RequestVoteCommandImpl extends CommandImpl implements RequestVoteCommand {

    private final RequestVoteEncoder encoder = new RequestVoteEncoder();

    public RequestVoteCommandImpl(PacketWriter packet) {
        super(packet);
        setEncoder(encoder);
    }

    @Override
    public RequestVoteCommand term(long term) {
         encoder.term(term);
         return this;
    }

    @Override
    public RequestVoteCommand candidateId(long instance) {
        encoder.candidateId(instance);
        return this;
    }

    @Override
    public RequestVoteCommand seqNo(long seqNo) {
        encoder.seqNo(seqNo);
        return this;
    }

    @Override
    public RequestVoteCommand logPosition(long position) {
        encoder.logPosition(position);
        return this;
    }
}
