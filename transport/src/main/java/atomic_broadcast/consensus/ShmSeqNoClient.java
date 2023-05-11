package atomic_broadcast.consensus;

import atomic_broadcast.utils.ShmFileConstants;
import org.agrona.IoUtil;
import org.agrona.concurrent.UnsafeBuffer;

import java.io.File;
import java.nio.MappedByteBuffer;

import static atomic_broadcast.utils.ShmFileConstants.SEQ_NUM_FILE_PREFIX;
import static atomic_broadcast.utils.ShmFileConstants.SHM_SUFFIX;

public class ShmSeqNoClient implements AutoCloseable {

    private final ConsensusStateSnapshotReader reader = new ConsensusStateSnapshotReader();
    private UnsafeBuffer buffer;
    private final MappedByteBuffer mmap;
    private final int INSTANCE_OFFSET = 0;
    private final int SEQ_NUM_OFFSET = INSTANCE_OFFSET + Long.BYTES;
    private final int LOG_POSITION_OFFSET = SEQ_NUM_OFFSET + Long.BYTES;
    private final int TERM_OFFSET = LOG_POSITION_OFFSET + Long.BYTES;
    private final int VOTED_OFFSET = TERM_OFFSET + Long.BYTES;

    public ShmSeqNoClient(int instance) {
        File file = new File(SEQ_NUM_FILE_PREFIX + instance + SHM_SUFFIX);
        if (file.exists()) {
            mmap = IoUtil.mapExistingFile(file, ShmFileConstants.SEQ_NUM_FILE);
        } else {
            mmap =  IoUtil.mapNewFile(file, ShmFileConstants.SEQ_NUM_FILE_SIZE_BYTES);
        }
        buffer = new UnsafeBuffer(mmap);
    }

    public ConsensusStateSnapshot readSeqNums() {
        reader.setSequencerConsensusState(
                buffer.getInt(INSTANCE_OFFSET),
                buffer.getLong(SEQ_NUM_OFFSET),
                buffer.getLong(LOG_POSITION_OFFSET),
                buffer.getLong(TERM_OFFSET),
                buffer.getLong(VOTED_OFFSET));
        return reader;
    }

    @Override
    public void close() throws Exception {
        IoUtil.unmap(mmap);
        buffer = null;
    }

    public void writeSeqNum(int instance, long seqNo, long logPosition) {
        buffer.putInt(INSTANCE_OFFSET, instance);
        buffer.putLong(SEQ_NUM_OFFSET, seqNo);
        buffer.putLong(LOG_POSITION_OFFSET, logPosition);
    }

    public void writeConsensusState(long term, long votedForInstance) {
        buffer.putLong(TERM_OFFSET, term);
        buffer.putLong(VOTED_OFFSET, votedForInstance);
    }
}
