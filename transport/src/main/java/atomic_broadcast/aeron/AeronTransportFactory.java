package atomic_broadcast.aeron;

import atomic_broadcast.client.CommandPublisher;
import atomic_broadcast.client.EventSubscriber;
import atomic_broadcast.transport.TransportFactory;
import atomic_broadcast.utils.*;

import static atomic_broadcast.aeron.AeronModule.COMMAND_STREAM_ID;

public class AeronTransportFactory implements TransportFactory {

    private final InstanceInfo instanceInfo;
    private final EventSubscriber eventSubscriber;
    private final CommandPublisher commandPublisher;
    private final AeronClient aeronClient;

    public AeronTransportFactory(InstanceInfo instanceInfo,
                                 TransportParams transportParams,
                                 AeronParams aeronParams,
                                 ModuleBuilder moduleBuilder) {
        this.instanceInfo = instanceInfo;
        this.aeronClient = new AeronClient(aeronParams, instanceInfo);
        this.eventSubscriber = new AeronTransportClient(aeronClient, transportParams, instanceInfo);
        this.commandPublisher = new AeronPublisherClient(aeronClient, transportParams.publicationChannel(), COMMAND_STREAM_ID);
        moduleBuilder.add(aeronClient);
    }

    @Override
    public CommandPublisher cmdPublisher() {
        return commandPublisher;
    }

    @Override
    public EventSubscriber eventSubscriber() {
        return eventSubscriber;
    }
}
