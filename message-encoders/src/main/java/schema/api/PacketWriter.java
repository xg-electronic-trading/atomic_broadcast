package schema.api;

import com.messages.sbe.MessageHeaderEncoder;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.sbe.MessageFlyweight;

import java.nio.ByteBuffer;

public class PacketWriter {
    MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(2 * 1024 * 1024));

    public int encodeHeader(MessageFlyweight messageFlyweight) {
        headerEncoder.wrap(buffer, 0);
        headerEncoder.blockLength(messageFlyweight.sbeBlockLength());
        headerEncoder.templateId(messageFlyweight.sbeTemplateId());
        headerEncoder.schemaId(messageFlyweight.sbeSchemaId());
        headerEncoder.version(messageFlyweight.sbeSchemaVersion());
        headerEncoder.seqNo(0);
        return headerEncoder.encodedLength();
    }

    public DirectBuffer buffer() {
        return buffer;
    }
}
