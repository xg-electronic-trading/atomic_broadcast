package atomic_broadcast.sequencer;

import atomic_broadcast.client.TransportSession;
import atomic_broadcast.utils.TransportParams;
import atomic_broadcast.utils.TransportState;
import org.agrona.concurrent.UnsafeBuffer;

import static atomic_broadcast.utils.TransportState.*;

public class SequencerTransportWorker implements TransportSession {

    private final TransportParams params;
    private final SequencerTransport transportClient;

    private TransportState state = NoState;

    public SequencerTransportWorker(TransportParams params, SequencerTransport transportClient) {
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
        switch (state) {
            case NoState:
                break;
            case ConnectToJournalSource:
                state = transportClient.connectToJournalSource() ? FindJournal : ConnectToJournalSource;
                break;
            case FindJournal:
                state = transportClient.findJournal() ? ConnectToEventStream : AdvertiseSeqNum;
                break;
            case ConnectToEventStream:
                state = transportClient.connectToEventStream() ? PollEventStream : ConnectToEventStream;
                break;
            case PollEventStream:
                transportClient.pollEventStream();
                break;
        }

        return true;
    }

    @Override
    public boolean publish(UnsafeBuffer buffer, int offset, int length) {
        return false;
    }
}
