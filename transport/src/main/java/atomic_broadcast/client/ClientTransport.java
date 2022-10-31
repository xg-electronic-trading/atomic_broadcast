package atomic_broadcast.client;


import atomic_broadcast.utils.TransportParams;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.UnsafeBuffer;

public class ClientTransport implements TransportSession, Agent {

    private TransportParams params;

    public ClientTransport(TransportParams params) {
        this.params = params;
    }

    @Override
    public boolean isSubscriptionConnected() {
        return false;
    }

    @Override
    public boolean isPublicationConnected() {
        return false;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean poll() {
        return false;
    }

    @Override
    public boolean publish(UnsafeBuffer buffer, int offset, int length) {
        return false;
    }

    @Override
    public int doWork() throws Exception {
        return 0;
    }

    @Override
    public String roleName() {
        return null;
    }
}
