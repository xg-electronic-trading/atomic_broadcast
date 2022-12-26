package atomic_broadcast.client;

public interface EventSubscriber extends AutoCloseable {

    boolean connectToJournalSource();

    boolean findJournal();

    boolean connectToEventStream();

    boolean pollEventStream();

    boolean pollReplay();

    boolean isSubscriptionConnected();

    boolean isSubscriptionClosed();

}
