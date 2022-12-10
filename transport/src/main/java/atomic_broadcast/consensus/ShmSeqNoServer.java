package atomic_broadcast.consensus;

import atomic_broadcast.utils.ShmFileConstants;
import org.agrona.IoUtil;
import org.agrona.concurrent.UnsafeBuffer;

import java.io.File;
import java.nio.MappedByteBuffer;

public class ShmSeqNoServer implements SeqNumSnapshotWriter {

    private UnsafeBuffer buffer;
    private final MappedByteBuffer mmap;

    private final String SEQ_NUM_FILE =
            ShmFileConstants.SHM_DIR +
            ShmFileConstants.SEQ_NUM_FILE +
            ShmFileConstants.SHM_SUFFIX;

    public ShmSeqNoServer() {
        File file = new File(SEQ_NUM_FILE);
        if (file.exists()) {
            mmap = IoUtil.mapExistingFile(file, ShmFileConstants.SEQ_NUM_FILE);
        } else {
            mmap =  IoUtil.mapNewFile(file, ShmFileConstants.SEQ_NUM_FILE_SIZE_BYTES);
        }

        buffer = new UnsafeBuffer(mmap);
    }


    @Override
    public void setReady(boolean isReady) {

    }

    @Override
    public void writeSeqNum(int component, int instance, int seqNo) {

    }
}
