package atomic_broadcast.client;

import atomic_broadcast.utils.TransportState;
import org.agrona.concurrent.UnsafeBuffer;

public interface TransportSession {

    boolean isSubscriptionConnected();

    boolean isPublicationConnected();

    void start();

    void stop();

    boolean poll();

    TransportState state();

    boolean publish(UnsafeBuffer buffer, int offset, int length);
}
