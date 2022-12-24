package atomic_broadcast.sequencer;

import org.agrona.concurrent.UnsafeBuffer;

public interface EventPublisher {

    /**
     * interface used by sequencer to publish commands received from command stream
     * onto event stream to be consumed by client applications.
     *
     *
     * @param buffer - buffer containing encoded event
     * @param offset - offset from which event begins in buffer
     * @param length - total encoded length of event.
     * @return
     */

    boolean publish(UnsafeBuffer buffer, int offset, int length);
}
