package atomic_broadcast;

import atomic_broadcast.client.ClientTransportWorker;
import atomic_broadcast.client.TransportClient;
import atomic_broadcast.client.TransportSession;
import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.TransportParams;

public class EventBusTransportModule implements Module {

    private final TransportClient transportClient;
    private final TransportParams params;

    private TransportSession transportSession;

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
        transportSession.stop();
    }

    @Override
    public void poll() {
        transportSession.poll();
    }
}
