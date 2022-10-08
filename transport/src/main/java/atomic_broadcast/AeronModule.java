package atomic_broadcast;

import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.OperatingSystem;
import io.aeron.Aeron;
import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.CloseHelper;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.NoOpIdleStrategy;

public class AeronModule implements Module {

    public static final String REPLICATION_CHANNEL = "aeron:udp?endpoint=localhost:0";
    public static final String CONTROL_REQUEST_CHANNEL = "aeron:udp?endpoint=localhost:8010";
    public static final String CONTROL_RESPONSE_CHANNEL = "aeron:udp?endpoint=localhost:0";
    private static final String AERON_DIR_NAME = "/Users/fa/sandbox/aeron";
    private static final String AERON_ARCHIVE_DIR_NAME = "/Users/fa/sandbox/aeron-archive";

    private final boolean startMediaDriverInProcess;
    private final boolean connectToMediaDriver;
    private final boolean lowLatencyMode;
    private final OperatingSystem os;

    private Aeron aeron;
    private ArchivingMediaDriver archivingMediaDriver;

    public AeronModule(boolean startMediaDriverInProcess, boolean connectToMediaDriver, boolean lowLatencyMode) {
        this.startMediaDriverInProcess = startMediaDriverInProcess;
        this.connectToMediaDriver = connectToMediaDriver;
        this.lowLatencyMode = lowLatencyMode;
        this.os = OperatingSystem.from(System.getProperty("os.name"));
    }


    @Override
    public void start() {
        if (startMediaDriverInProcess) {

            MediaDriver.Context ctx = new MediaDriver.Context();
            Archive.Context archiveCtx = new Archive.Context();
            archiveCtx.recordingEventsEnabled(false)
                    .controlChannel(CONTROL_REQUEST_CHANNEL)
                    .replicationChannel(REPLICATION_CHANNEL);

            ctx.termBufferSparseFile(false)
                    .spiesSimulateConnection(true);

            switch (os) {
                case MacOS:
                    ctx.aeronDirectoryName(AERON_DIR_NAME);
                    archiveCtx.aeronDirectoryName(ctx.aeronDirectoryName());
                    archiveCtx.archiveDirectoryName(AERON_ARCHIVE_DIR_NAME);
                default:

            }

            if(lowLatencyMode) {
                ctx.threadingMode(ThreadingMode.DEDICATED)
                        .conductorIdleStrategy(BusySpinIdleStrategy.INSTANCE)
                        .receiverIdleStrategy(NoOpIdleStrategy.INSTANCE)
                        .senderIdleStrategy(NoOpIdleStrategy.INSTANCE);

                archiveCtx.threadingMode(ArchiveThreadingMode.DEDICATED);

            } else {
                ctx.threadingMode(ThreadingMode.SHARED);
                archiveCtx.threadingMode(ArchiveThreadingMode.SHARED);
            }



            archivingMediaDriver = ArchivingMediaDriver.launch(ctx, archiveCtx);
            System.out.println("launched media driver");
        }

        if (connectToMediaDriver) {
            aeron = Aeron.connect(
                    new Aeron.Context()
                            .aeronDirectoryName(AERON_DIR_NAME));

            System.out.println("connected to media driver");
        }
    }

    @Override
    public void close() {
        CloseHelper.closeAll(
                aeron,
                archivingMediaDriver,
                () -> archivingMediaDriver.archive().context().deleteDirectory(),
                () -> archivingMediaDriver.mediaDriver().context().deleteDirectory());

        System.out.println("closed media driver");
    }

    @Override
    public void poll() {

    }
}
