package atomic_broadcast.client;

import atomic_broadcast.utils.TransportParams;
import atomic_broadcast.utils.TransportState;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import org.agrona.concurrent.UnsafeBuffer;

import static atomic_broadcast.utils.TransportState.*;

public class ClientTransportWorker implements TransportSession {

    private static final Log log = LogFactory.getLog(ClientTransportWorker.class.getName());

    private final TransportParams params;
    private final TransportClient transportClient;
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
        try {
            transportClient.close();
        } catch (Exception e) {
            log.error().append("error whilst closing: ").appendLast(e);
        }
    }

    @Override
    public boolean poll() {
        doWork();
        return true;
    }

    @Override
    public TransportState state() {
        return state;
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
                state = transportClient.findJournal() ? StartReplayMerge : FindJournal;
                break;
            case StartReplayMerge:
                state = transportClient.connectToEventStream() ? PollEventStream : StartReplayMerge;
                break;
            case PollEventStream:
                transportClient.pollEventStream();
                break;
        }

        return state.getCode();
    }
}
