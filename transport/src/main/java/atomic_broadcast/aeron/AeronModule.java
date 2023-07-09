package atomic_broadcast.aeron;

import atomic_broadcast.utils.InstanceInfo;
import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.ModuleName;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import io.aeron.Aeron;
import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.archive.client.AeronArchive;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.CloseHelper;
import org.agrona.concurrent.BusySpinIdleStrategy;;
import org.agrona.concurrent.NoOpIdleStrategy;

import java.util.function.Predicate;

import static atomic_broadcast.utils.ModuleName.AeronMediaDriver;

public class AeronModule implements Module {

    Log log = LogFactory.getLog(this.getClass().getName());

    public static final String REPLICATION_CHANNEL = "aeron:udp?endpoint=localhost:0";
    public static final String LOCAL_ENDPOINT = "aeron:udp?endpoint=localhost:";
    public static final String CONTROL_RESPONSE_CHANNEL = "aeron:udp?endpoint=localhost:0";
    public static final String REPLAY_CHANNEL = "aeron:udp?endpoint=localhost:6666";
    public static final String COMMAND_ENDPOINT = "localhost:40001";
    public static final String DYNAMIC_ENDPOINT = "localhost:0";
    public static final String LOCAL_HOST = "localhost";
    public static final int CONSENSUS_PORT_RANGE_START = 41000;
    public static final int EVENT_STREAM_CONTROL_PORT = 40002;
    public static final int ARCHIVE_REQUEST_PORT_RANGE_START = 8010;
    public static final int EVENT_STREAM_ID = 10_000_000;
    public static final int COMMAND_STREAM_ID = 20_000_000;
    public static final int CONSENSUS_STREAM_ID = 30_000_000;
    public static final String MULTICAST_ADDRESS = "239.1.1.1"; //mock multicast address
    public static final String MULTICAST_EVENT_STREAM_ENDPOINT = MULTICAST_ADDRESS + ":" + EVENT_STREAM_CONTROL_PORT;

    private ArchivingMediaDriver archivingMediaDriver;
    private AeronParams params;
    private final InstanceInfo instanceInfo;

    public AeronModule(AeronParams params, InstanceInfo instanceInfo) {
        this.params = params;
        this.instanceInfo = instanceInfo;
    }

    @Override
    public InstanceInfo instanceInfo() {
        return instanceInfo;
    }

    @Override
    public ModuleName name() {
        return AeronMediaDriver;
    }

    @Override
    public void start() {
        MediaDriver.Context ctx = new MediaDriver.Context();
        Archive.Context archiveCtx = new Archive.Context();
        archiveCtx.recordingEventsEnabled(false)
                .controlChannel(LOCAL_ENDPOINT+params.archiveRequestPort())
                .archiveClientContext(new AeronArchive.Context().controlResponseChannel(CONTROL_RESPONSE_CHANNEL))
                .replicationChannel(REPLICATION_CHANNEL)
                .epochClock(params.clock())
                .nanoClock(params.clock());

        ctx.termBufferSparseFile(false)
                .spiesSimulateConnection(true)
                .epochClock(params.clock())
                .nanoClock(params.clock());

        ctx.aeronDirectoryName(params.aeronDir());
        archiveCtx.aeronDirectoryName(ctx.aeronDirectoryName());
        archiveCtx.archiveDirectoryName(params.aeronDir() + "/" + "archive");

        if(params.lowLatencyMode()) {
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
        log.info().append("launched media driver with request port: ").appendLast(params.archiveRequestPort());
    }

    @Override
    public void close() {
        CloseHelper.closeAll(
                archivingMediaDriver,
                this::deletearchiveDir,
                this::deleteMediaDriverDir);

        log.info().appendLast("closed aeron");
        log.info().appendLast("closed media driver");
    }

    private void deletearchiveDir() {
        if (null != archivingMediaDriver) {
            archivingMediaDriver.archive().context().deleteDirectory();
        }
    }

    private void deleteMediaDriverDir() {
        if (null != archivingMediaDriver) {
            archivingMediaDriver.mediaDriver().context().deleteDirectory();
        }
    }
}
