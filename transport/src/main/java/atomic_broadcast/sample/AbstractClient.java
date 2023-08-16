package atomic_broadcast.sample;

import atomic_broadcast.client.*;
import atomic_broadcast.listener.MessageListener;
import atomic_broadcast.transport.TransportFactory;
import atomic_broadcast.utils.*;

public abstract class AbstractClient {

    private InstanceInfo instanceInfo;
    private final EventReaderModule eventReaderModule;
    private final ClientPublisherModule clientPublisherModule;
    private final CommandProcessor commandProcessor;

    /**
     *  The abstract client constructs a module that is able to connect to the
     *  sequenced message bus with the ability to send commands and receive events.
     *
     */

    public AbstractClient(InstanceInfo instanceInfo,
                          TransportParams transportParams,
                          TransportFactory transportFactory,
                          MessageListener messageListener,
                          ModuleBuilder moduleBuilder,
                          PollableBuilder pollableBuilder) {
        this.instanceInfo = instanceInfo;
        this.eventReaderModule = new EventReaderModule(
                transportFactory.eventSubscriber(),
                transportParams,
                messageListener,
                instanceInfo);

        CommandPublisher cmdPublisher = transportFactory.cmdPublisher();
        this.clientPublisherModule = new ClientPublisherModule(cmdPublisher, transportParams, instanceInfo);
        this.commandProcessor = new CommandProcessorImpl(cmdPublisher, new NoOpCommandValidator(), instanceInfo);

        if (eventReaderModule.eventReaderType() != EventReaderType.Direct) {
            pollableBuilder.add(eventReaderModule.transport());
        }

        pollableBuilder.add(clientPublisherModule.transport());
        pollableBuilder.add(eventReaderModule.eventsReader());
        moduleBuilder.add(eventReaderModule);
        moduleBuilder.add(clientPublisherModule);
    }

    protected CommandProcessor commandProcessor() {
        return commandProcessor;
    }

    protected MessageListener messageListener() {
        return eventReaderModule.listener();
    }
}
