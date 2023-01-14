package schema.api;

import com.messages.sbe.MessageHeaderDecoder;
import com.messages.sbe.MessageHeaderEncoder;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

public class PacketReader implements Packet {
    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final UnsafeBuffer mutableBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(2 * 1024 * 1024));

    public void wrap(DirectBuffer buffer, int offset) {
        mutableBuffer.wrap(buffer);
        headerEncoder.wrap(mutableBuffer, offset);
        headerDecoder.wrap(mutableBuffer, offset);
    }

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

    public void encodeSeqNo(long seqNo) {
        headerEncoder.seqNo(seqNo);
    }

}
