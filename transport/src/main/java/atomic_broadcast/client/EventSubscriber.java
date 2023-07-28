package atomic_broadcast.client;

public interface EventSubscriber extends AutoCloseable {

    boolean connectToJournalSource();

    boolean findJournal();

    boolean connectToEventStream();

    boolean startTailEventJournal();

    boolean pollEventStream();

    boolean pollReplay();

    boolean pollJournal();

    boolean isSubscriptionConnected();

    boolean isSubscriptionClosed();

}
