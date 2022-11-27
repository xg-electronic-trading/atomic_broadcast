package atomic_broadcast.client;


import atomic_broadcast.utils.TransportParams;
import atomic_broadcast.utils.TransportState;
import org.agrona.concurrent.UnsafeBuffer;

import static atomic_broadcast.utils.TransportState.*;

public class ClientTransportWorker implements TransportSession {

    private TransportParams params;
    private TransportClient transportClient;
    private TransportState state = NoState;

    public ClientTransportWorker(TransportParams params, TransportClient transportClient) {
        this.params = params;
        this.transportClient = transportClient;
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
        state = ConnectToJournalSource;
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean poll() {
        doWork();
        return true;
    }

    @Override
    public boolean publish(UnsafeBuffer buffer, int offset, int length) {
        return false;
    }


    private int doWork() {
        switch (state) {
            case NoState:
                break;
            case ConnectToJournalSource:
                state = transportClient.connectToJournalSource() ? FindJournal : ConnectToJournalSource;
                break;
            case FindJournal:
                state = transportClient.findJournal() ? ConnectToEventStream : FindJournal;
                break;
            case ConnectToEventStream:
                state = transportClient.connectToEventStream() ? PollEventStream : ConnectToEventStream;
                break;
            case PollEventStream:
                transportClient.pollEventStream();
                break;
        }

        return state.getCode();
    }
}
