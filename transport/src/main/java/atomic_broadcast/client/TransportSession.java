package atomic_broadcast.client;

import org.agrona.concurrent.UnsafeBuffer;

public interface TransportSession {

    boolean isSubscriptionConnected();

    boolean isPublicationConnected();

    void start();

    void stop();

    boolean pollSubscription();

    boolean publish(UnsafeBuffer buffer, int offset, int length);
}
