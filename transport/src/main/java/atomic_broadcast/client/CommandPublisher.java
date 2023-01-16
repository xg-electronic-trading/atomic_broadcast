package atomic_broadcast.client;

import org.agrona.DirectBuffer;

public interface CommandPublisher extends AutoCloseable {
    boolean connectToCommandStream();
    boolean isPublicationConnected();
    boolean isPublicationClosed();
    boolean send(DirectBuffer buffer, int offset, int length);

}
