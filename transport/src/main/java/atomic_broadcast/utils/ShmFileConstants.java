package atomic_broadcast.utils;

import org.agrona.SystemUtil;

public class ShmFileConstants {

    static {
        if (SystemUtil.isLinux()) {
            SHM_DIR = "/dev/shm/";
        } else {
            SHM_DIR = SystemUtil.tmpDirName();
        }
    }

    public static String SHM_DIR;
    public static final String SHM_SUFFIX = ".dat";
    public static final String SEQ_NUM_FILE = "seq-nums";
    public static final String MKT_DATA_FILE = "mkt-data";


    public static final int SEQ_NUM_FILE_SIZE_BYTES = 1024;

}
