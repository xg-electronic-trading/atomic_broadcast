package atomic_broadcast.client;

import atomic_broadcast.utils.*;
import atomic_broadcast.utils.Module;

import static atomic_broadcast.utils.ModuleName.ClientTransport;

public class EventBusSubscriberModule implements Module {

    private final TransportParams params;

    private final TransportWorker transportSession;

    public EventBusSubscriberModule(TransportClient transportClient, TransportParams params) {
        this.params = params;
        switch (params.connectAs()) {
            case Client:
                transportSession = new ClientTransportWorker(params, transportClient);
                break;
            default:
                throw new IllegalArgumentException("error: trying to connect as: " + params.connectAs());
        }
    }

    @Override
    public ModuleName name() {
        return ClientTransport;
    }

    @Override
    public void start() {
        transportSession.start();
    }

    @Override
    public void close() {
        transportSession.close();
    }


    public Pollable transport() {
        return transportSession;
    }

    public TransportState state() { return transportSession.state(); }

    public TransportParams params() { return params; }
}
