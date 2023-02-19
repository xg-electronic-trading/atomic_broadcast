package atomic_broadcast.consensus;

import atomic_broadcast.utils.ShmFileConstants;
import com.messages.sbe.BooleanType;
import org.agrona.IoUtil;
import org.agrona.concurrent.UnsafeBuffer;

import java.io.File;
import java.nio.MappedByteBuffer;

public class ShmSeqNoClient implements AutoCloseable {

    private SeqNumSnapshotReader reader = new SeqNumSnapshotReader();
    private UnsafeBuffer buffer;
    private final MappedByteBuffer mmap;
    private final int IS_READY_OFFSET = 0;
    private final int INSTANCE_OFFSET = IS_READY_OFFSET + 1;
    private final int SEQ_NUM_OFFSET = INSTANCE_OFFSET + Long.BYTES;
    private final int instance;

    private final String SEQ_NUM_FILE =
            ShmFileConstants.SHM_DIR +
            ShmFileConstants.SEQ_NUM_FILE +
            ShmFileConstants.SHM_SUFFIX;

    public ShmSeqNoClient(int instance) {
        this.instance = instance;
        File file = new File(SEQ_NUM_FILE);
        if (file.exists()) {
            mmap = IoUtil.mapExistingFile(file, ShmFileConstants.SEQ_NUM_FILE);
            buffer = new UnsafeBuffer(mmap);
        } else {
            mmap =  IoUtil.mapNewFile(file, ShmFileConstants.SEQ_NUM_FILE_SIZE_BYTES);
            buffer = new UnsafeBuffer(mmap);
            writeSeqNum(true, instance, 0);
        }
    }

    public SeqNumSnapshot readSeqNums() {
        BooleanType booleanType = BooleanType.get(buffer.getByte(IS_READY_OFFSET));
        reader.setReady(booleanType == BooleanType.T);
        reader.setInstanceSeqNum(buffer.getInt(INSTANCE_OFFSET), buffer.getLong(SEQ_NUM_OFFSET));

        return reader;
    }

    @Override
    public void close() throws Exception {
        IoUtil.unmap(mmap);
        buffer = null;
    }

    public void writeSeqNum(boolean isReady, int instance, long seqNo) {
        buffer.putByte(IS_READY_OFFSET, isReady ? (byte) 1: (byte) 0);
        buffer.putInt(INSTANCE_OFFSET, instance);
        buffer.putLong(SEQ_NUM_OFFSET, seqNo);
    }
}
