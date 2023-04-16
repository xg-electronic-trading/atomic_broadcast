package atomic_broadcast.consensus;

import atomic_broadcast.utils.ShmFileConstants;
import com.messages.sbe.BooleanType;
import org.agrona.IoUtil;
import org.agrona.concurrent.UnsafeBuffer;

import java.io.File;
import java.nio.MappedByteBuffer;

import static atomic_broadcast.utils.ShmFileConstants.SHM_SUFFIX;

public class ShmSeqNoClient implements AutoCloseable {

    private SeqNumSnapshotReader reader = new SeqNumSnapshotReader();
    private UnsafeBuffer buffer;
    private final MappedByteBuffer mmap;
    private final int INSTANCE_OFFSET = 0;
    private final int SEQ_NUM_OFFSET = INSTANCE_OFFSET + Long.BYTES;
    private final int instance;

    private final String SEQ_NUM_FILE_PREFIX =
            ShmFileConstants.SHM_DIR +
            ShmFileConstants.SEQ_NUM_FILE;

    public ShmSeqNoClient(int instance) {
        this.instance = instance;
        File file = new File(SEQ_NUM_FILE_PREFIX + "-" + instance + SHM_SUFFIX);
        if (file.exists()) {
            mmap = IoUtil.mapExistingFile(file, ShmFileConstants.SEQ_NUM_FILE);
        } else {
            mmap =  IoUtil.mapNewFile(file, ShmFileConstants.SEQ_NUM_FILE_SIZE_BYTES);
        }
        buffer = new UnsafeBuffer(mmap);
    }

    public SeqNumSnapshot readSeqNums() {
        reader.setInstanceSeqNum(buffer.getInt(INSTANCE_OFFSET), buffer.getLong(SEQ_NUM_OFFSET));

        return reader;
    }

    @Override
    public void close() throws Exception {
        IoUtil.unmap(mmap);
        buffer = null;
    }

    public void writeSeqNum(int instance, long seqNo) {
        buffer.putInt(INSTANCE_OFFSET, instance);
        buffer.putLong(SEQ_NUM_OFFSET, seqNo);
    }
}
