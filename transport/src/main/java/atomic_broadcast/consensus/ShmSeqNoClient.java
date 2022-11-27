package atomic_broadcast.consensus;

import atomic_broadcast.utils.ShmFileConstants;
import org.agrona.IoUtil;
import org.agrona.concurrent.UnsafeBuffer;

import java.io.File;
import java.nio.MappedByteBuffer;

public class ShmSeqNoClient implements AutoCloseable {

    private SeqNumSnapshotWriter writer = new SeqNumSnapshotWriter();
    private UnsafeBuffer buffer;
    private final MappedByteBuffer mmap;
    private final int IS_READY_OFFSET = 0;
    private final int NUM_OF_SEQUENCERS_OFFSET = 8;

    private final String SEQ_NUM_FILE =
            ShmFileConstants.SHM_DIR +
            ShmFileConstants.SEQ_NUM_FILE +
            ShmFileConstants.SHM_SUFFIX;

    public ShmSeqNoClient() {
        File file = new File(SEQ_NUM_FILE);
        IoUtil.checkFileExists(file, ShmFileConstants.SEQ_NUM_FILE);
        mmap = IoUtil.mapExistingFile(file, ShmFileConstants.SEQ_NUM_FILE);
        buffer = new UnsafeBuffer(mmap);
    }

    public SeqNumSnapshot readSeqNums() {
        writer.setIsReady(true);

        return writer;
    }

    @Override
    public void close() throws Exception {
        IoUtil.unmap(mmap);
        buffer = null;
    }
}
