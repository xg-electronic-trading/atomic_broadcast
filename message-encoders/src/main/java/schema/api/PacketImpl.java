package schema.api;

import com.messages.sbe.MessageHeaderDecoder;
import com.messages.sbe.MessageHeaderEncoder;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

public abstract class PacketImpl implements Packet {

    protected final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    protected final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    protected final UnsafeBuffer mutableBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(2 * 1024 * 1024));


    public int fixedMessageLength() {
        return headerDecoder.blockLength();
    }

    public int messageType() {
        return headerDecoder.templateId();
    }

    public int schemaId() {
        return headerDecoder.schemaId();
    }

    public int version() {
        return headerDecoder.version();
    }

    public long seqNo() {
        return headerDecoder.seqNo();
    }

    @Override
    public int offset() {
        return headerDecoder.encodedLength();
    }

    public DirectBuffer buffer() {
        return mutableBuffer;
    }
}
