package atomic_broadcast.client;

import atomic_broadcast.utils.Pollable;
import atomic_broadcast.utils.TransportState;
import org.agrona.concurrent.UnsafeBuffer;

public interface TransportWorker extends Pollable {

    void start();

    void close();

    TransportState state();
}
