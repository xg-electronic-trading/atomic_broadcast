package atomic_broadcast.client;

public interface TransportClient {

    boolean connectToJournalSource();

    boolean findJournal();

    boolean connectToEventStream();

    boolean pollEventStream();

}
