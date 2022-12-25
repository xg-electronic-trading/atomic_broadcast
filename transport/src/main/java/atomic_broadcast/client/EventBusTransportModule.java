package atomic_broadcast.client;

import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.TransportParams;
import atomic_broadcast.utils.TransportState;

public class EventBusTransportModule implements Module {

    private final TransportClient transportClient;
    private final TransportParams params;

    private TransportWorker transportSession;

    public EventBusTransportModule(TransportClient transportClient, TransportParams params) {
        this.transportClient = transportClient;
        this.params = params;
    }


    @Override
    public void start() {
        switch (params.connectAs()) {
            case Client:
                transportSession = new ClientTransportWorker(params, transportClient);
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
}
