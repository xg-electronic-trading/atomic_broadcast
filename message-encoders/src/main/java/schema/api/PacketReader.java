package schema.api;

import org.agrona.DirectBuffer;

public class PacketReader extends PacketImpl {

    public void wrap(DirectBuffer buffer, int offset, int length) {
        mutableBuffer.wrap(buffer, offset, length);
        headerEncoder.wrap(mutableBuffer, 0);
        headerDecoder.wrap(mutableBuffer, 0);
    }

    public void encodeSeqNo(long seqNo) {
        headerEncoder.seqNo(seqNo);
    }

    public void encodeIsReplay(int isReplay) { headerEncoder.isReplay((short) isReplay); }

    public boolean isReplay() {
        return headerDecoder.isReplay() == 1;
    }
}
