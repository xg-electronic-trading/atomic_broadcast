package atomic_broadcast.utils;

public class ShmFileConstants {

    public static final String SHM_DIR = System.getenv("HOME") + "/sandbox/shm/";
    public static final String SHM_SUFFIX = ".dat";
    public static final String SEQ_NUM_FILE = "seq-nums";
    public static final String MKT_DATA_FILE = "mkt-data";


    public static final int SEQ_NUM_FILE_SIZE_BYTES = 10 * 1024;

}
