package schema.api;

import command.Command;
import org.agrona.sbe.MessageEncoderFlyweight;

public abstract class CommandImpl implements Command {

    protected final PacketWriter packet;
    protected MessageEncoderFlyweight encoder;

    public CommandImpl(PacketWriter packet) {
        this.packet = packet;
    }

    protected void setEncoder(MessageEncoderFlyweight encoder) {
        this.encoder = encoder;
    }

    @Override
    public void beginWrite() {
        int headerLength = packet.encodeHeader(encoder);
        encoder.wrap(packet.buffer(), headerLength);
    }

    @Override
    public void endWrite() {
        packet.reset(encoder.encodedLength());
    }

    @Override
    public Packet packet() {
        return packet;
    }

    @Override
    public int encodedLength() {
       return encoder.encodedLength();
    }

    @Override
    public String toString() {
        return encoder.toString();
    }
}
