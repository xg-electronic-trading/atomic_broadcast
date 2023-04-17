package atomic_broadcast;

import atomic_broadcast.aeron.RecordingDescriptor;
import atomic_broadcast.aeron.RecordingDescriptorConsumerImpl;
import atomic_broadcast.aeron.RecordingSignalConsumerImpl;
import io.aeron.*;
import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.ArchivingMediaDriver;
import io.aeron.archive.client.AeronArchive;
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
import java.nio.ByteBuffer;

import static io.aeron.Aeron.NULL_VALUE;
import static io.aeron.archive.client.RecordingSignalPoller.FRAGMENT_LIMIT;
import static io.aeron.archive.codecs.SourceLocation.LOCAL;

public class ReplayMergeFromReplicatedArchiveTest {


    private static final int STREAM_ID = 1033;

    private static final String CONTROL_ENDPOINT = "localhost:23265";
    private static final String LIVE_ENDPOINT = "localhost:0";
    private static final String LIVE_ENDPOINT_2 = "localhost:0";
    private static final String REPLAY_ENDPOINT = "localhost:0";

    public static final String REPLICATION_CHANNEL = "aeron:udp?endpoint=localhost:0";
    public static final String CONTROL_REQUEST_CHANNEL = "aeron:udp?endpoint=localhost:8010";
    public static final String DST_CONTROL_REQUEST_CHANNEL = "aeron:udp?endpoint=localhost:8015";
    public static final String CONTROL_RESPONSE_CHANNEL = "aeron:udp?endpoint=localhost:0";

    /**
     * Note: controlEndpoint and controlMode set when running tests locally. This ensures
     * tests pub-sub using MDC when multicast is not available.
     */

    private final String publicationChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .controlEndpoint(CONTROL_ENDPOINT) //change this to endpoint and remove control mode when using multicast
            .controlMode(CommonContext.MDC_CONTROL_MODE_DYNAMIC)
            .build();

    private final String liveDestination = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .controlEndpoint(CONTROL_ENDPOINT) // remove control endpoint when using multicast
            .endpoint(LIVE_ENDPOINT)
            .build();

    private final String replayDestination = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .endpoint(REPLAY_ENDPOINT)
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
    public void replayMerge() {
        Publication pub = aeron.addPublication(publicationChannel, STREAM_ID);
        System.out.println("publication session-id: " + pub.sessionId());

        final String subscriptionChannel = new ChannelUriStringBuilder()
                .media(CommonContext.UDP_MEDIA)
                .controlMode(CommonContext.MDC_CONTROL_MODE_MANUAL)
                .sessionId(pub.sessionId())
                .build();

        aeronArchive.startRecording(publicationChannel, STREAM_ID, LOCAL, true);
        final CountersReader counters = aeron.countersReader();
        final int recordingCounterId = awaitRecordingCounterId(counters, pub.sessionId());
        final long recordingId = RecordingPos.getRecordingId(counters, recordingCounterId);
        pollForSignals(aeronArchive, recordingSignalConsumer, RecordingSignal.START);

        int messagesToPublish = 5;
        for (int i = 0; i < messagesToPublish; i++) {
            buffer.putLong(0, i);
            pub.offer(buffer, 0, Long.BYTES);
        }

        final String replayChannel = new ChannelUriStringBuilder()
                .media(CommonContext.UDP_MEDIA)
                .sessionId(pub.sessionId())
                .build();

         replayMerge(
                 recordingId,
                 subscriptionChannel,
                 replayChannel,
                 replayDestination,
                 liveDestination,
                 aeronArchive
                 );
    }

    @Test
    public void replayMergeFromReplicateRecording() {
        Publication pub = aeron.addPublication(publicationChannel, STREAM_ID);
        System.out.println("publication session-id: " + pub.sessionId());

        aeronArchive.startRecording(publicationChannel, STREAM_ID, LOCAL, true);
        final CountersReader counters = aeron.countersReader();
        final int recordingCounterId = awaitRecordingCounterId(counters, pub.sessionId());
        final long recordingId = RecordingPos.getRecordingId(counters, recordingCounterId);
        pollForSignals(aeronArchive, recordingSignalConsumer, RecordingSignal.START);

        int messagesToPublish = 5;
        for (int i = 0; i < messagesToPublish; i++) {
            buffer.putLong(0, i);
            pub.offer(buffer, 0, Long.BYTES);
        }

        dstAeronArchive.replicate(
                recordingId,
                NULL_VALUE,
                AeronArchive.Configuration.CONTROL_STREAM_ID_DEFAULT,
                CONTROL_REQUEST_CHANNEL,
                publicationChannel);

        pollForSignals(dstAeronArchive, dstRecordingSignalConsumer, RecordingSignal.REPLICATE);
        pollForSignals(dstAeronArchive, dstRecordingSignalConsumer, RecordingSignal.EXTEND);
        pollForSignals(dstAeronArchive, dstRecordingSignalConsumer, RecordingSignal.MERGE);

        RecordingDescriptorConsumerImpl descriptorConsumer = new RecordingDescriptorConsumerImpl();
        aeronArchive.listRecordings(0, Integer.MAX_VALUE, descriptorConsumer);

        RecordingDescriptor srcRecording = descriptorConsumer.getRecordingDescriptors().get(0);
        System.out.println("src archive recording: " + srcRecording);

        RecordingDescriptorConsumerImpl dstDescriptorConsumer = new RecordingDescriptorConsumerImpl();
        dstAeronArchive.listRecordings(0, Integer.MAX_VALUE, dstDescriptorConsumer);
        RecordingDescriptor dstRecording = dstDescriptorConsumer.getRecordingDescriptors().get(0);
        System.out.println("dst archive recording: " + dstRecording);

        final String subscriptionChannel = new ChannelUriStringBuilder()
                .media(CommonContext.UDP_MEDIA)
                .controlMode(CommonContext.MDC_CONTROL_MODE_MANUAL)
                .sessionId(dstRecording.sessionId()) // session-id of dst-recording and src-recording should match pub session-id
                .build();

        String replayChannel = new ChannelUriStringBuilder()
                .media(CommonContext.UDP_MEDIA)
                .sessionId(dstRecording.sessionId()) // session-id of dst-recording and src-recording should match pub session-id
                .build();

        String liveDestination = new ChannelUriStringBuilder()
                .media(CommonContext.UDP_MEDIA)
                .controlEndpoint(CONTROL_ENDPOINT)
                .endpoint(LIVE_ENDPOINT_2)
                .build();

        replayMerge(
                dstRecording.recordingId(),
                subscriptionChannel,
                replayChannel,
                replayDestination,
                liveDestination,
                dstAeronArchive);

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

    private void replayMerge(long recordingId,
                             String subscriptionChannel,
                             String replayChannel,
                             String replayDestination,
                             String liveDestination,
                             AeronArchive aeronArchive) {
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
