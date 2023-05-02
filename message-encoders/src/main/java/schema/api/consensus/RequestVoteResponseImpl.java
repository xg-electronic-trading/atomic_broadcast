package schema.api.consensus;

import com.messages.sbe.BooleanType;
import com.messages.sbe.RequestVoteResponseDecoder;
import schema.api.Packet;

public class RequestVoteResponseImpl implements RequestVoteResponse {

    private final RequestVoteResponseDecoder decoder = new RequestVoteResponseDecoder();

    public void init(Packet packet) {
        decoder.wrap(packet.buffer(),
                packet.offset(),
                packet.fixedMessageLength(),
                packet.version()
                );
    }

    @Override
    public long instanceId() {
        return decoder.instanceId();
    }

    @Override
    public long term() {
        return decoder.term();
    }

    @Override
    public boolean voteGranted() {
        return decoder.voteGranted() == BooleanType.T;
    }

    public String toString() {
        return decoder.toString();
    }
}
