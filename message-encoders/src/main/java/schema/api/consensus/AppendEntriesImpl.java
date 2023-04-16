package schema.api.consensus;

import com.messages.sbe.AppendEntriesDecoder;
import schema.api.Packet;

public class AppendEntriesImpl implements AppendEntries {

    private final AppendEntriesDecoder decoder = new AppendEntriesDecoder();

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
    public long leaderId() {
        return decoder.leaderId();
    }
}
