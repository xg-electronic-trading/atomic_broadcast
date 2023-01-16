package schema.api;

import org.agrona.MutableDirectBuffer;
import org.agrona.sbe.MessageFlyweight;

public class PacketWriter extends PacketImpl {

    public int encodeHeader(MessageFlyweight messageFlyweight) {
        headerEncoder.wrap(mutableBuffer, 0);
        headerDecoder.wrap(mutableBuffer, 0);

        headerEncoder.blockLength(messageFlyweight.sbeBlockLength());
        headerEncoder.templateId(messageFlyweight.sbeTemplateId());
        headerEncoder.schemaId(messageFlyweight.sbeSchemaId());
        headerEncoder.version(messageFlyweight.sbeSchemaVersion());
        headerEncoder.seqNo(0);
        return headerEncoder.encodedLength();
    }

    public MutableDirectBuffer buffer() {
        return mutableBuffer;
    }

    public void reset(int length) {
        mutableBuffer.setMemory(0, length, (byte) 0);
    }
}
