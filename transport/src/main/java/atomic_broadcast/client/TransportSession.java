package atomic_broadcast.client;

public interface TransportSession {

    boolean isSubscriptionConnected();

    boolean isPublicationConnected();
}
