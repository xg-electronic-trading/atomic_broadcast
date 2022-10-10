package atomic_broadcast.utils;

import io.aeron.archive.client.RecordingDescriptorConsumer;

import java.util.ArrayList;

public class RecordingDescriptorConsumerImpl implements RecordingDescriptorConsumer {

    private ArrayList<RecordingDescriptor> recordingDescriptors = new ArrayList<>(20);

    @Override
    public void onRecordingDescriptor(long controlSessionId, long correlationId, long recordingId, long startTimestamp, long stopTimestamp, long startPosition, long stopPosition, int initialTermId, int segmentFileLength, int termBufferLength, int mtuLength, int sessionId, int streamId, String strippedChannel, String originalChannel, String sourceIdentity) {
        RecordingDescriptor descriptor = new RecordingDescriptor();

        recordingDescriptors.add(descriptor.set(
                controlSessionId,
                correlationId,
                recordingId,
                startTimestamp,
                stopTimestamp,
                startPosition,
                stopPosition,
                initialTermId,
                segmentFileLength,
                termBufferLength,
                mtuLength,
                sessionId,
                streamId,
                strippedChannel,
                originalChannel,
                sourceIdentity)
        );
    }

    public ArrayList<RecordingDescriptor> getRecordingDescriptors() {
        return recordingDescriptors;
    }
}
