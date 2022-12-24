package atomic_broadcast.listener;

import org.agrona.DirectBuffer;

public interface MessageListener {
    void onMessage(DirectBuffer buffer, int offset, int length, long seqNum, boolean isReplay);
}
