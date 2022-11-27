package atomic_broadcast.aeron;

import io.aeron.archive.client.RecordingSignalConsumer;
import io.aeron.archive.codecs.RecordingSignal;

public class RecordingSignalConsumerImpl implements RecordingSignalConsumer {

    private RecordingSignal signal = RecordingSignal.NULL_VAL;

    @Override
    public void onSignal(long controlSessionId, long correlationId, long recordingId, long subscriptionId, long position, RecordingSignal signal) {
        System.out.println("received recording signal: "+ signal);
        this.signal = signal;
    }

    public RecordingSignal getSignal() {
        return signal;
    }
}
