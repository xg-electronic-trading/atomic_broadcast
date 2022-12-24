package atomic_broadcast.client;

import atomic_broadcast.utils.TransportState;
import org.agrona.concurrent.UnsafeBuffer;

public interface TransportWorker {

    void start();

    void close();

    boolean poll();

    TransportState state();
}
