package atomic_broadcast.client;

public interface TransportClient extends AutoCloseable {

    boolean connectToJournalSource();

    boolean findJournal();

    boolean connectToEventStream();

    boolean pollEventStream();

    boolean isSubscriptionConnected();

    boolean isSubscriptionClosed();

}
