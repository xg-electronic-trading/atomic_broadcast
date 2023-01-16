package atomic_broadcast.client;

import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.ModuleName;
import atomic_broadcast.utils.TransportParams;
import atomic_broadcast.utils.TransportState;

import static atomic_broadcast.utils.ModuleName.ClientPublisher;

public class ClientPublisherModule implements Module {

    private final CommandPublisher commandPublisher;
    private final TransportParams params;

    private TransportWorker transportSession;

    public ClientPublisherModule(CommandPublisher commandPublisher, TransportParams params) {
        this.commandPublisher = commandPublisher;
        this.params = params;
    }

    @Override
    public ModuleName name() {
        return ClientPublisher;
    }

    @Override
    public void start() {
        switch (params.connectAs()) {
            case Client:
                transportSession = new ClientPublicationWorker(params, commandPublisher);
                break;

            default:
                throw new IllegalArgumentException("error: trying to connect as: " + params.connectAs());
        }

        transportSession.start();
    }

    @Override
    public void close() {
        transportSession.close();
    }

    @Override
    public void poll() {
        transportSession.poll();
    }

    public TransportState state() { return transportSession.state(); }

    public CommandPublisher cmdPublisher() { return commandPublisher; }
}
