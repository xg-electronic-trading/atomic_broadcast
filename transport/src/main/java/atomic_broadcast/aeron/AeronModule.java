package atomic_broadcast.aeron;

import atomic_broadcast.utils.ConnectAs;
import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.OperatingSystem;
import atomic_broadcast.utils.TransportParams;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.codecs.RecordingSignal;
import io.aeron.archive.codecs.SourceLocation;
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
    public static final String REMOTE_CONTROL_REQUEST_CHANNEL = "aeron:udp?endpoint=localhost:8011";
    public static final String CONTROL_RESPONSE_CHANNEL = "aeron:udp?endpoint=localhost:0";
    public static final String COMMAND_ENDPOINT = "localhost:40001";
    public static final String CONTROL_ENDPOINT = "localhost:23265";
    public static final String DYNAMIC_ENDPOINT = "localhost:0";
    public static final int EVENT_STREAM_ID = 10_000_000;
    public static final int COMMAND_STREAM_ID = 20_000_000;
    private static final String AERON_DIR_NAME = "/Users/fa/sandbox/shm/aeron";
    private static final String AERON_ARCHIVE_DIR_NAME = "/Users/fa/sandbox/shm/aeron-archive";

    private final boolean startMediaDriverInProcess;
    private final boolean connectToMediaDriver;
    private final boolean lowLatencyMode;
    private final OperatingSystem os;

    private Aeron aeron;
    private ArchivingMediaDriver archivingMediaDriver;

    private AeronArchive.AsyncConnect asyncConnect;
    private AeronArchive aeronArchive;
    private AeronArchive srcAeronArchive; // this is the archive instance to replicate from.

    private final RecordingDescriptorConsumerImpl recordingDescriptorConsumer = new RecordingDescriptorConsumerImpl();
    private final RecordingSignalConsumerImpl recordingSignalConsumer = new RecordingSignalConsumerImpl();
    private final RecordingDescriptor recordingDescriptor = new RecordingDescriptor();

    private long replicationSessionId = Aeron.NULL_VALUE;
    private long recordingSubscriptionId = Aeron.NULL_VALUE;

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
                    .archiveClientContext(new AeronArchive.Context().controlResponseChannel(CONTROL_RESPONSE_CHANNEL))
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
            log.info().appendLast("launched media driver");
        }

        if (connectToMediaDriver) {
            aeron = Aeron.connect(
                    new Aeron.Context()
                            .aeronDirectoryName(AERON_DIR_NAME));

            log.info().appendLast("connected to media driver");
        }
    }

    @Override
    public void close() {
        CloseHelper.closeAll(
                srcAeronArchive,
                aeronArchive,
                aeron,
                archivingMediaDriver,
                () -> archivingMediaDriver.archive().context().deleteDirectory(),
                () -> archivingMediaDriver.mediaDriver().context().deleteDirectory());

        log.info().appendLast("closed src archive");
        log.info().appendLast("closed local archive");
        log.info().appendLast("closed aeron");
        log.info().appendLast("closed media driver");
    }

    @Override
    public void poll() {

    }

    private AeronArchive.Context createNewArchiveCtx(String controlRequestChannel) {
        return new AeronArchive.Context()
                .controlRequestChannel(controlRequestChannel)
                .controlResponseChannel(CONTROL_RESPONSE_CHANNEL)
                .recordingSignalConsumer(recordingSignalConsumer)
                .aeron(aeron);
    }

    public boolean connectToArchive() {
        if (null != aeronArchive) {
            return true;
        }
        aeronArchive = connectToArchive(createNewArchiveCtx(CONTROL_REQUEST_CHANNEL));
        return null != aeronArchive;
    }

    private boolean connectToSrcArchive() {
        if(null != srcAeronArchive) {
            return true;
        }
        srcAeronArchive = connectToArchive(createNewArchiveCtx(REMOTE_CONTROL_REQUEST_CHANNEL));
        return null != srcAeronArchive;
    }

    private AeronArchive connectToArchive(AeronArchive.Context ctx) {
        AeronArchive archive = null;
        if (null == asyncConnect) {
            asyncConnect = AeronArchive.asyncConnect(ctx);
        } else {
            archive = asyncConnect.poll();
            if(null != archive) {
                asyncConnect = null;
            }
        }

        return archive;
    }

    public RecordingDescriptor findRecording() {
        return findRecording(aeronArchive);
    }

    private RecordingDescriptor findRecording(AeronArchive archive) {
        recordingDescriptorConsumer.getRecordingDescriptors().clear(); //will generate garbage when emptying. not used in steady state

        if (null !=  archive) {
            int recordingsFound = archive.listRecordings(0, Integer.MAX_VALUE, recordingDescriptorConsumer);
            if (recordingsFound > 0) {
               Optional<RecordingDescriptor> recordingOpt = recordingDescriptorConsumer.getRecordingDescriptors().stream().max(Comparator.comparing(RecordingDescriptor::startTimestamp));
               return recordingOpt.orElse(recordingDescriptor);
            }
        } else {
            log.error().appendLast("cannot find recording, aeron archive client is null");
        }

        return recordingDescriptor;
    }

    public boolean startReplication(TransportParams params, RecordingDescriptor dstRecordingId) {
        if (params.connectAs() == ConnectAs.Client) {
            throw new UnsupportedOperationException("client application cannot perform replication.");
        } else {
            boolean isConnectedToSrcArchive = connectToSrcArchive();
            if (isConnectedToSrcArchive) {
                RecordingDescriptor srcRecording = findRecording(srcAeronArchive);
                replicationSessionId = aeronArchive.replicate(
                        srcRecording.recordingId(),
                        dstRecordingId.recordingId(),
                        AeronArchive.Configuration.CONTROL_STREAM_ID_DEFAULT,
                        REMOTE_CONTROL_REQUEST_CHANNEL,
                        null //populate pubchannel to merge back to live stream.
                );

                return Aeron.NULL_VALUE != replicationSessionId;
            } else {
                return false;
            }
        }
    }

    public void closeReplication() {
        if (Aeron.NULL_VALUE != replicationSessionId) {
            aeronArchive.stopReplication(replicationSessionId);
            replicationSessionId =  Aeron.NULL_VALUE;
        }
    }

    public Subscription addSubscription(String channel, int stream) {
        return aeron.addSubscription(channel, stream);
    }

    public void closeSubscription(Subscription subscription) {
        if (null != subscription) {
          subscription.close();
        }
    }

    public Publication addPublication(String channel, int stream) {
        return aeron.addPublication(channel, stream);
    }

    public Publication addExclusivePublication(String channel, int stream) {
        return aeron.addExclusivePublication(channel, stream);
    }

    public void closePublication(Publication publication) {
        if (null != publication) {
            publication.close();
        }
    }

    public boolean startRecording(String channel, int stream) {
        if (Aeron.NULL_VALUE == recordingSubscriptionId) {
            recordingSubscriptionId = aeronArchive.startRecording(channel, stream, SourceLocation.LOCAL);
        }

        return Aeron.NULL_VALUE != recordingSubscriptionId;
    }

    public void closeRecording() {
        if (recordingSubscriptionId != Aeron.NULL_VALUE) {
           aeronArchive.stopRecording(recordingSubscriptionId);
           recordingSubscriptionId = Aeron.NULL_VALUE;
        }
    }

    public boolean pollForRecordingSignal(RecordingSignal signal) {
        return pollForRecordingSignal(aeronArchive, signal);
    }

    private boolean pollForRecordingSignal(AeronArchive archive, RecordingSignal signal) {
        if (recordingSignalConsumer.getSignal() != signal) {
            archive.pollForRecordingSignals();
            return false;
        } else {
            log.info().append("recording signal: ").appendLast(recordingSignalConsumer.getSignal());
            return true;
        }
    }

    public AeronArchive aeronArchive() {
        return aeronArchive;
    }
}
