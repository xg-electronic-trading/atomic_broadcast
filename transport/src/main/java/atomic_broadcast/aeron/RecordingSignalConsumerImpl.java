package atomic_broadcast.aeron;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import io.aeron.archive.client.RecordingSignalConsumer;
import io.aeron.archive.codecs.RecordingSignal;

public class RecordingSignalConsumerImpl implements RecordingSignalConsumer {

    private static final Log log = LogFactory.getLog(RecordingSignalConsumerImpl.class.getName());

    private RecordingSignal signal = RecordingSignal.NULL_VAL;

    @Override
    public void onSignal(long controlSessionId, long correlationId, long recordingId, long subscriptionId, long position, RecordingSignal signal) {
        log.info().append("received recording signal: ").appendLast(signal);
        this.signal = signal;
    }

    public RecordingSignal getSignal() {
        return signal;
    }
}
