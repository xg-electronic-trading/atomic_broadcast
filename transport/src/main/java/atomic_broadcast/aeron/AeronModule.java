package atomic_broadcast.aeron;

import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.OperatingSystem;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.archive.client.AeronArchive;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.CloseHelper;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.NoOpIdleStrategy;

import java.util.Comparator;
import java.util.Optional;

public class AeronModule implements Module {

    Log log = LogFactory.getLog(this.getClass().getName());

    public static final String REPLICATION_CHANNEL = "aeron:udp?endpoint=localhost:0";
    public static final String CONTROL_REQUEST_CHANNEL = "aeron:udp?endpoint=localhost:8010";
    public static final String CONTROL_RESPONSE_CHANNEL = "aeron:udp?endpoint=localhost:0";
    private static final String AERON_DIR_NAME = "/Users/fa/sandbox/shm/aeron";
    private static final String AERON_ARCHIVE_DIR_NAME = "/Users/fa/sandbox/shm/aeron-archive";

    private final boolean startMediaDriverInProcess;
    private final boolean connectToMediaDriver;
    private final boolean lowLatencyMode;
    private final OperatingSystem os;

    private Aeron aeron;
    private ArchivingMediaDriver archivingMediaDriver;

    private AeronArchive.Context archiveClientCtx;
    private AeronArchive.AsyncConnect asyncConnect;
    private AeronArchive aeronArchive;

    private final RecordingDescriptorConsumerImpl recordingDescriptorConsumer = new RecordingDescriptorConsumerImpl();
    private RecordingDescriptor recordingDescriptor = new RecordingDescriptor();

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

            archiveClientCtx = new AeronArchive.Context()
                    .controlRequestChannel(controlRequestChannel())
                    .controlResponseChannel(controlResponseChannel())
                    .aeron(aeron);

            System.out.println("connected to media driver");
        }
    }

    @Override
    public void close() {
        CloseHelper.closeAll(
                aeronArchive,
                aeron,
                archivingMediaDriver,
                () -> archivingMediaDriver.archive().context().deleteDirectory(),
                () -> archivingMediaDriver.mediaDriver().context().deleteDirectory());

        System.out.println("closed media driver");
    }

    @Override
    public void poll() {

    }

    public String controlRequestChannel() {
        return CONTROL_REQUEST_CHANNEL;
    }

    public String controlResponseChannel() {
        return CONTROL_RESPONSE_CHANNEL;
    }

    public boolean connectToArchive() {
        if (null == asyncConnect) {
            asyncConnect = AeronArchive.asyncConnect(archiveClientCtx);
        } else {
            aeronArchive = asyncConnect.poll();
            if(null != aeronArchive) {
                asyncConnect = null;
                return true;
            }
        }

        return false;
    }

    public RecordingDescriptor findRecording() {
        recordingDescriptorConsumer.getRecordingDescriptors().clear(); //will generate garbage when emptying. not used in steady state

        if (null !=  aeronArchive) {
            int recordingsFound = aeronArchive.listRecordings(0, Integer.MAX_VALUE, recordingDescriptorConsumer);
            if (recordingsFound > 0) {
               Optional<RecordingDescriptor> recordingOpt = recordingDescriptorConsumer.getRecordingDescriptors().stream().max(Comparator.comparing(RecordingDescriptor::startTimestamp));
               return recordingOpt.orElse(recordingDescriptor);
            }
        } else {
            log.error().appendLast("cannot find recording, aeron archive client is null");
        }

        return recordingDescriptor;
    }

    public Subscription addSubscription(String channel, int stream) {
        return aeron.addSubscription(channel, stream);
    }

    public Publication addPublication(String channel, int stream) {
        return aeron.addPublication(channel, stream);
    }

    public AeronArchive aeronArchive() {
        return aeronArchive;
    }
}
