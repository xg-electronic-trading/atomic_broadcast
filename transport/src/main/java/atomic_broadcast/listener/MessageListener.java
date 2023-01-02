package atomic_broadcast.listener;

import schema.api.Packet;

public interface MessageListener {
    void onMessage(Packet packet, boolean isReplay);
}
