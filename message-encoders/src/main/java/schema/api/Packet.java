package schema.api;

import org.agrona.DirectBuffer;

public interface Packet {
    boolean isReplay();
    int fixedMessageLength();
    int messageType();
    int schemaId();
    int version();
    long seqNo();
    int offset();
    DirectBuffer buffer();
}
