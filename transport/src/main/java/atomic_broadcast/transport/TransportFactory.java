package atomic_broadcast.transport;

import atomic_broadcast.client.CommandPublisher;
import atomic_broadcast.client.EventSubscriber;

public interface TransportFactory {

    CommandPublisher cmdPublisher();

    EventSubscriber eventSubscriber();
}
