package atomic_broadcast.client;

public interface CommandPublisher {
    boolean connectToCommandStream();
    boolean isPublicationConnected();
    boolean isPublicationClosed();

}
