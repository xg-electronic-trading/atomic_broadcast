package atomic_broadcast;

import atomic_broadcast.utils.RecordingSignalConsumerImpl;
import io.aeron.*;
import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.client.RecordingDescriptorConsumer;
import io.aeron.archive.client.RecordingSignalConsumer;
import io.aeron.archive.client.ReplayMerge;
import io.aeron.archive.codecs.RecordingSignal;
import io.aeron.archive.status.RecordingPos;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.CloseHelper;
import org.agrona.SystemUtil;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.status.CountersReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import static io.aeron.Aeron.NULL_VALUE;
import static io.aeron.archive.client.RecordingSignalPoller.FRAGMENT_LIMIT;
import static io.aeron.archive.codecs.SourceLocation.REMOTE;

public class ReplicatedRecordingTest {


    private static final int STREAM_ID = 1033;

    private static final String CONTROL_ENDPOINT = "localhost:23265";
    private static final String RECORDING_ENDPOINT = "localhost:23266";
    private static final String LIVE_ENDPOINT = "localhost:23267";
    private static final String REPLAY_ENDPOINT = "localhost:0";

    public static final String REPLICATION_CHANNEL = "aeron:udp?endpoint=localhost:0";
    public static final String CONTROL_REQUEST_CHANNEL = "aeron:udp?endpoint=localhost:8010";
    public static final String DST_CONTROL_REQUEST_CHANNEL = "aeron:udp?endpoint=localhost:8015";
    public static final String CONTROL_RESPONSE_CHANNEL = "aeron:udp?endpoint=localhost:0";

    private static final int PUBLICATION_TAG = 2;

    private final String publicationChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .tags("1," + PUBLICATION_TAG)
            .controlEndpoint(CONTROL_ENDPOINT)
            .controlMode(CommonContext.MDC_CONTROL_MODE_DYNAMIC)
            //.termLength(TERM_LENGTH)
            //.taggedFlowControl(GROUP_TAG, 1, "5s")
            .build();

    private final String liveDestination = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .endpoint(LIVE_ENDPOINT)
            .controlEndpoint(CONTROL_ENDPOINT)
            .build();

    private final String replayDestination = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .endpoint(REPLAY_ENDPOINT)
            .build();

    private final String replayChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .isSessionIdTagged(true)
            .sessionId(PUBLICATION_TAG)
            .build();


    private ArchivingMediaDriver mediaDriver;
    private Aeron aeron;
    private AeronArchive aeronArchive;

    private ArchivingMediaDriver dstMediaDriver;
    private Aeron dstAeron;
    private AeronArchive dstAeronArchive;

    private RecordingSignalConsumerImpl recordingSignalConsumer;
    private RecordingSignalConsumerImpl dstRecordingSignalConsumer;

    private UnsafeBuffer buffer;


    private final FragmentHandler fragmentHandler =  new FragmentAssembler(
        (buffer, offset, length, header) -> {
            long i = buffer.getLong(offset);
            System.out.println("received message: " + i);
        }
    );


    @BeforeEach
    public void before() {
        buffer = new UnsafeBuffer(ByteBuffer.allocate(1024));

        String aeronDir = SystemUtil.tmpDirName() + "/aeron";
        String dstAeronDir = SystemUtil.tmpDirName() + "/aeron-dst";

        String archiveDir = SystemUtil.tmpDirName() + "/aeron-archive";
        String dstArchiveDir = SystemUtil.tmpDirName() + "/aeron-archive-dst";

        MediaDriver.Context ctx = new MediaDriver.Context();
        MediaDriver.Context dstCtx = new MediaDriver.Context();

        Archive.Context archiveCtx = new Archive.Context();
        Archive.Context dstArchiveCtx = new Archive.Context();


        ctx.termBufferSparseFile(false)
                .spiesSimulateConnection(true)
                .aeronDirectoryName(aeronDir)
                .threadingMode(ThreadingMode.SHARED);

        dstCtx.termBufferSparseFile(false)
                .spiesSimulateConnection(true)
                .aeronDirectoryName(dstAeronDir)
                .threadingMode(ThreadingMode.SHARED);

        archiveCtx.recordingEventsEnabled(false)
                .controlChannel(CONTROL_REQUEST_CHANNEL)
                .archiveClientContext(new AeronArchive.Context().controlResponseChannel(CONTROL_RESPONSE_CHANNEL))
                .replicationChannel(REPLICATION_CHANNEL)
                .threadingMode(ArchiveThreadingMode.SHARED)
                .archiveDirectoryName(archiveDir)
                .aeronDirectoryName(aeronDir);

        dstArchiveCtx.recordingEventsEnabled(false)
                .controlChannel(DST_CONTROL_REQUEST_CHANNEL)
                .archiveClientContext(new AeronArchive.Context().controlResponseChannel(CONTROL_RESPONSE_CHANNEL))
                .replicationChannel(REPLICATION_CHANNEL)
                .threadingMode(ArchiveThreadingMode.SHARED)
                .archiveDirectoryName(dstArchiveDir)
                .aeronDirectoryName(dstAeronDir);


        mediaDriver = ArchivingMediaDriver.launch(ctx, archiveCtx);
        dstMediaDriver = ArchivingMediaDriver.launch(dstCtx, dstArchiveCtx);

        System.out.println("launched media driver");

        aeron = Aeron.connect(new Aeron.Context().aeronDirectoryName(aeronDir));
        dstAeron = Aeron.connect(new Aeron.Context().aeronDirectoryName(dstAeronDir));

        System.out.println("launched aeron");

        recordingSignalConsumer = new RecordingSignalConsumerImpl();
        dstRecordingSignalConsumer = new RecordingSignalConsumerImpl();

        aeronArchive = AeronArchive.connect(
                new AeronArchive.Context()
                .controlRequestChannel(CONTROL_REQUEST_CHANNEL)
                .controlResponseChannel(CONTROL_RESPONSE_CHANNEL)
                .recordingSignalConsumer(recordingSignalConsumer)
                .aeron(aeron)
        );

        dstAeronArchive = AeronArchive.connect(
                new AeronArchive.Context()
                        .controlRequestChannel(DST_CONTROL_REQUEST_CHANNEL)
                        .controlResponseChannel(CONTROL_RESPONSE_CHANNEL)
                        .recordingSignalConsumer(dstRecordingSignalConsumer)
                        .aeron(dstAeron)
        );

        System.out.println("launched archive");

    }


    @Test
    public void replayMergeFromReplicatedRecording() {
        Publication pub = aeron.addPublication(publicationChannel, STREAM_ID);

        final String recordingChannel = new ChannelUriStringBuilder()
                .media(CommonContext.UDP_MEDIA)
                .endpoint(RECORDING_ENDPOINT)
                .controlEndpoint(CONTROL_ENDPOINT)
                .sessionId(pub.sessionId())
                //.groupTag(GROUP_TAG)
                .build();


        final String subscriptionChannel = new ChannelUriStringBuilder()
                .media(CommonContext.UDP_MEDIA)
                .controlMode(CommonContext.MDC_CONTROL_MODE_MANUAL)
                .sessionId(pub.sessionId())
                .build();

        aeronArchive.startRecording(recordingChannel, STREAM_ID, REMOTE, true);
        final CountersReader counters = aeron.countersReader();
        final int recordingCounterId = awaitRecordingCounterId(counters, pub.sessionId());
        final long recordingId = RecordingPos.getRecordingId(counters, recordingCounterId);
        pollForSignals(aeronArchive, recordingSignalConsumer, RecordingSignal.START);

        int messagesToPublish = 5;
        for (int i = 0; i < messagesToPublish; i++) {
            buffer.putLong(0, i);
            pub.offer(buffer, 0, Long.BYTES);
        }

         replayMerge(recordingId, subscriptionChannel);
    }

    private void pollForSignals(AeronArchive aeronArchive, RecordingSignalConsumerImpl recordingSignalConsumer, RecordingSignal expectedSignal) {
        while (recordingSignalConsumer.getSignal() != expectedSignal) {
            aeronArchive.pollForRecordingSignals();
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void replayMerge(long recordingId, String subscriptionChannel) {
        Subscription subscription = aeron.addSubscription(subscriptionChannel, STREAM_ID);

        ReplayMerge replayMerge = new ReplayMerge(
             subscription,
             aeronArchive,
             replayChannel,
             replayDestination,
             liveDestination,
             recordingId,
             0
        );

        while (!replayMerge.isMerged()) {
            if (0 == replayMerge.poll(fragmentHandler, FRAGMENT_LIMIT)) {
                if(replayMerge.hasFailed()) {
                    System.out.println("replay merge has failed");
                }
            }
        }

        System.out.println(replayMerge);
    }

    private int awaitRecordingCounterId(final CountersReader counters, final int sessionId)
    {
        int counterId;
        while (NULL_VALUE == (counterId = RecordingPos.findCounterIdBySession(counters, sessionId)))
        {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return counterId;
    }


    @AfterEach
    public void after() {
        CloseHelper.closeAll(
                dstAeronArchive,
                aeronArchive,
                dstAeron,
                aeron,
                dstMediaDriver,
                mediaDriver,
                () -> dstMediaDriver.archive().context().deleteDirectory(),
                () -> dstMediaDriver.mediaDriver().context().deleteDirectory(),
                () -> mediaDriver.archive().context().deleteDirectory(),
                () -> mediaDriver.mediaDriver().context().deleteDirectory()
        );

        System.out.println("closed archive, aeron and media driver");
    }


}
