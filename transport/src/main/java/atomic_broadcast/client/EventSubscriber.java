package atomic_broadcast.client;

import atomic_broadcast.utils.JournalState;

public interface EventSubscriber extends AutoCloseable {

    boolean connectToJournalSource();

    JournalState findJournal();

    boolean connectToEventStream();

    boolean startTailEventJournal();

    boolean pollEventStream();

    boolean pollReplay();

    boolean pollJournal();

    boolean isSubscriptionConnected();

    boolean isSubscriptionClosed();

}
