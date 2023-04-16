package command;

import com.messages.sbe.AppendEntriesEncoder;
import schema.api.CommandImpl;
import schema.api.PacketWriter;

public class AppendEntriesCommandimpl extends CommandImpl implements AppendEntriesCommand {

    private final AppendEntriesEncoder encoder = new AppendEntriesEncoder();

    public AppendEntriesCommandimpl(PacketWriter packet) {
        super(packet);
        setEncoder(encoder);
    }

    @Override
    public AppendEntriesCommand term(long term) {
        encoder.term(term);
        return this;
    }

    @Override
    public AppendEntriesCommand leaderId(long leaderId) {
        encoder.leaderId(leaderId);
        return this;
    }
}
