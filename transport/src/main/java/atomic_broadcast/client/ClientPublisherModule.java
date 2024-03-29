package atomic_broadcast.client;

import atomic_broadcast.utils.*;
import atomic_broadcast.utils.Module;

import static atomic_broadcast.utils.ModuleName.ClientPublisher;

public class ClientPublisherModule implements Module {

    private final CommandPublisher commandPublisher;
    private final TransportWorker transportSession;
    private final InstanceInfo instanceInfo;

    private boolean started = false;

    public ClientPublisherModule(CommandPublisher commandPublisher,
                                 TransportParams params,
                                 InstanceInfo instanceInfo) {
        this.commandPublisher = commandPublisher;
        this.instanceInfo = instanceInfo;
        switch (params.connectAs()) {
            case ClusterClient:
            case Client:
                transportSession = new ClientPublicationWorker(params, commandPublisher, instanceInfo);
                break;
            default:
                throw new IllegalArgumentException("error: trying to connect as: " + params.connectAs());
        }
    }

    @Override
    public ModuleName name() {
        return ClientPublisher;
    }

    @Override
    public InstanceInfo instanceInfo() {
        return instanceInfo;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void start() {
        transportSession.start();
        started = true;
    }

    @Override
    public void close() {
        transportSession.close();
        started = false;
    }


    public Pollable transport() {
        return transportSession;
    }

    public TransportState state() { return transportSession.state(); }

    public CommandPublisher cmdPublisher() { return commandPublisher; }
}
