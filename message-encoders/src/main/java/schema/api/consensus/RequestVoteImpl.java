package schema.api.consensus;

import com.messages.sbe.RequestVoteDecoder;
import schema.api.Packet;

public class RequestVoteImpl implements RequestVote {

    private final RequestVoteDecoder decoder = new RequestVoteDecoder();

    public void init(Packet packet) {
        decoder.wrap(packet.buffer(),
                packet.offset(),
                packet.fixedMessageLength(),
                packet.version());
    }

    @Override
    public long term() {
        return decoder.term();
    }

    @Override
    public long candidateId() {
        return decoder.candidateId();
    }

    @Override
    public long seqNo() {
        return decoder.seqNo();
    }

    @Override
    public long logPosition() {
        return decoder.logPosition();
    }

    public String toString() {
        return decoder.toString();
    }
}
