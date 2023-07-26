package atomic_broadcast.aeron;

import atomic_broadcast.consensus.ClusterMember;
import atomic_broadcast.utils.*;
import atomic_broadcast.utils.Module;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import io.aeron.*;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.codecs.RecordingSignal;
import io.aeron.archive.codecs.SourceLocation;
import org.agrona.CloseHelper;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;

import java.io.File;
import java.util.Comparator;
import java.util.Optional;

import static atomic_broadcast.aeron.AeronModule.*;
import static atomic_broadcast.aeron.AeronModule.DYNAMIC_ENDPOINT;
import static atomic_broadcast.utils.ConnectUsing.Multicast;
import static atomic_broadcast.utils.ConnectUsing.Unicast;
import static atomic_broadcast.utils.ModuleName.AeronClient;

public class AeronClient implements Module {

    Log log = LogFactory.getLog(this.getClass().getName());

    private boolean started = false;

    public static final String LOCAL_ENDPOINT = "aeron:udp?endpoint=localhost:";
    public static final String REMOTE_ENDPOINT = "aeron:udp?endpoint=somehost:";

    private final InstanceInfo instanceInfo;
    private final AeronParams params;
    private Aeron aeron;
    private AeronArchive.AsyncConnect asyncConnect;
    private AeronArchive aeronArchive;
    private AeronArchive srcAeronArchive; // this is the archive instance to replicate from.

    private final AeronErrorHandler aeronErrorHandler = new AeronErrorHandler();
    private final AeronErrorHandler archiveErrorHandler = new AeronErrorHandler();
    private final AeronErrorHandler srcArchiveErrorHandler = new AeronErrorHandler();

    private long replicationSessionId = Aeron.NULL_VALUE;
    private long recordingSubscriptionId = Aeron.NULL_VALUE;

    private long replaySessionId = Aeron.NULL_VALUE;

    private final RecordingDescriptorConsumerImpl recordingDescriptorConsumer = new RecordingDescriptorConsumerImpl();
    private final RecordingSignalConsumerImpl recordingSignalConsumer = new RecordingSignalConsumerImpl();
    private final RecordingDescriptor recordingDescriptor = new RecordingDescriptor();
    private final IdleStrategy aeronDirIdleStrategy = new SleepingMillisIdleStrategy(1_000L);

    @Override
    public ModuleName name() {
        return AeronClient;
    }

    public AeronClient(AeronParams params, InstanceInfo instanceInfo) {
        this.params = params;
        this.instanceInfo = instanceInfo;
    }

    public InstanceInfo instanceInfo() {
        return instanceInfo;
    }

    @Override
    public boolean isStarted() {
        return started;
    }


    @Override
    public void start() {
        log.info().append("waiting for aeron dir creation: ").appendLast(params.aeronDir());
        awaitTillAeronDirExists();
        aeron = Aeron.connect(
                new Aeron.Context()
                        .aeronDirectoryName(params.aeronDir())
                        .errorHandler(aeronErrorHandler));

        log.info().appendLast("connected to media driver");
        started = true;
    }

    private void awaitTillAeronDirExists() {
        //do not connect until aeron dir exists
        File file = new File(params.aeronDir());
        while (!file.exists()) {
            aeronDirIdleStrategy.idle();
        }
    }

    @Override
    public void close() {
        CloseHelper.closeAll(
                srcAeronArchive,
                aeronArchive,
                aeron
        );

        log.info().appendLast("closed src archive");
        log.info().appendLast("closed local archive");
        log.info().appendLast("closed aeron");
        started = false;
    }

    private AeronArchive.Context createNewArchiveCtx(String controlRequestChannel, AeronErrorHandler errorHandler) {
        errorHandler.reset();
        return new AeronArchive.Context()
                .controlRequestChannel(controlRequestChannel)
                .controlResponseChannel(CONTROL_RESPONSE_CHANNEL)
                .recordingSignalConsumer(recordingSignalConsumer)
                .aeron(aeron)
                .errorHandler(errorHandler);
    }

    public boolean connectToArchive() {
        if (null != aeronArchive) {
            return true;
        }
        String localArchiveChannel = LOCAL_ENDPOINT + params.archiveRequestPort();
        aeronArchive = connectToArchive(createNewArchiveCtx(localArchiveChannel, archiveErrorHandler));
        return null != aeronArchive;
    }

    private boolean connectToSrcArchive(String remoteArchiveChannel) {
        if(null != srcAeronArchive) {
            return true;
        }

        srcAeronArchive = connectToArchive(createNewArchiveCtx(remoteArchiveChannel, srcArchiveErrorHandler));
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
        return findRecording(aeronArchive, false);
    }

    public RecordingDescriptor findActiveRecording() {
        return findRecording(aeronArchive, true);
    }

    private RecordingDescriptor findRecording(AeronArchive archive, boolean requireActive) {
        recordingDescriptorConsumer.getRecordingDescriptors().clear(); //will generate garbage when emptying. not used in steady state

        if (null !=  archive) {
            int recordingsFound = archive.listRecordings(0, Integer.MAX_VALUE, recordingDescriptorConsumer);
            if (recordingsFound > 0) {
                Optional<RecordingDescriptor> recordingOpt = recordingDescriptorConsumer.getRecordingDescriptors()
                        .stream()
                        .filter(r -> !requireActive || r.stopPosition() == Aeron.NULL_VALUE)
                        .max(Comparator.comparing(RecordingDescriptor::startTimestamp));
                return recordingOpt.orElse(recordingDescriptor);
            }
        } else {
            log.error().appendLast("cannot find recording, aeron archive client is null");
        }

        return recordingDescriptor;
    }

    public boolean startReplication(TransportParams transportParams,
                                    RecordingDescriptor dstRecording,
                                    ClusterMember leader) {
        if (transportParams.connectAs() == ConnectAs.Client) {
            throw new UnsupportedOperationException("client application cannot perform replication.");
        } else if (Aeron.NULL_VALUE == replicationSessionId) {
            String endpoint = leader.hostname() + ":" + leader.archivePort();
            String remoteArchiveChannel = new ChannelUriStringBuilder()
                    .media(CommonContext.UDP_MEDIA)
                    .endpoint(endpoint)
                    .build();

            boolean isConnectedToSrcArchive = connectToSrcArchive(remoteArchiveChannel); // need to set correct archive port of src
            if (isConnectedToSrcArchive) {
                RecordingDescriptor srcRecording = findRecording(srcAeronArchive, true);
                if (srcRecording.recordingId() != Aeron.NULL_VALUE) {
                    String liveDestination = liveEventStreamDestination(transportParams, leader.hostname());
                    replicationSessionId = aeronArchive.replicate(
                            srcRecording.recordingId(),
                            dstRecording.recordingId(),
                            AeronArchive.Configuration.CONTROL_STREAM_ID_DEFAULT,
                            remoteArchiveChannel,
                            liveDestination //populate pubchannel to merge back to live stream.
                    );
                }
            }
        }

        return Aeron.NULL_VALUE != replicationSessionId;
    }

    public void closeReplication() {
        checkReplicationDone();
        if (Aeron.NULL_VALUE != replicationSessionId) {
            aeronArchive.stopReplication(replicationSessionId);
            replicationSessionId = Aeron.NULL_VALUE;
        }
    }

    public void checkReplicationDone() {
        if (Aeron.NULL_VALUE != replicationSessionId) {
            boolean isDone = pollForRecordingSignal(RecordingSignal.REPLICATE_END);
            if (isDone) {
                replicationSessionId = Aeron.NULL_VALUE;
            }
        }
    }

    /**
     * Use this method for tailing a recording (journal).
     * Use cases:
     *  a) follower sequencer tailing its replicate journal to keep its state machine up to date (warm-standby)
     *  b) tailing a journal for order replay/debugging/logging
     *  c) slow consumer can process events from journal instead of using network subscription (e.g. unicast/multicast)
     */
    public boolean startOpenEndedReplay(RecordingDescriptor latestRecording, String replayChannel) {
        if (Aeron.NULL_VALUE == replaySessionId) {
            replaySessionId = aeronArchive.startReplay(
                    latestRecording.recordingId(),
                    latestRecording.startPosition(),
                    Long.MAX_VALUE, //indicate to follow live recording.
                    replayChannel,
                    EVENT_STREAM_ID
            );
        }

        return replaySessionId != Aeron.NULL_VALUE;
    }

    public boolean startReplay(RecordingDescriptor latestRecording, String replayChannel) {
        if (Aeron.NULL_VALUE == replaySessionId) {
            replaySessionId = aeronArchive.startReplay(
                    latestRecording.recordingId(),
                    latestRecording.startPosition(),
                    latestRecording.stopPosition(),
                    replayChannel,
                    EVENT_STREAM_ID
            );
        }

        return replaySessionId != Aeron.NULL_VALUE;
    }

    public void closeReplay() {
        if (Aeron.NULL_VALUE != replaySessionId) {
            aeronArchive.stopReplay(replaySessionId);
            replicationSessionId = Aeron.NULL_VALUE;
        }
    }

    public long replaySessionId() {
        if (Aeron.NULL_VALUE != replaySessionId){
            return replaySessionId;
        } else {
            throw new IllegalStateException("replay session id is null");
        }
    }

    public Subscription addSubscription(String channel, int stream) {
        log.info().append("app: ").append(instanceInfo.app())
                .append(", instance: ").append(instanceInfo.instance())
                .append(", adding subscription on channel-stream: ")
                .append(channel).append("-").appendLast(stream);
        return aeron.addSubscription(channel, stream);
    }

    public void closeSubscription(Subscription subscription) {
        if (null != subscription) {
            subscription.close();
            log.info().append("closed subscription: ").append(subscription.channel())
                    .append(":").appendLast(subscription.streamId());
        }
    }

    public Publication addPublication(String channel, int stream) {
        log.info().append("app: ").append(instanceInfo.app())
                .append(", instance: ").append(instanceInfo.instance())
                .append(", adding publication on channel-stream: ")
                .append(channel).append("-").appendLast(stream);
        return aeron.addPublication(channel, stream);
    }

    public Publication addExclusivePublication(String channel, int stream) {
        log.info().append("app: ").append(instanceInfo.app())
                .append(", instance: ").append(instanceInfo.instance())
                .append(", adding exclusive publication on channel-stream: ")
                .append(channel).append("-").appendLast(stream);
        return aeron.addExclusivePublication(channel, stream);
    }

    public void closePublication(Publication publication) {
        if (null != publication) {
            publication.close();
            log.info().append("closed publication: ").append(publication.channel())
                    .append(":").appendLast(publication.streamId());
        }
    }

    public boolean startRecording(String channel, int stream) {
        if (Aeron.NULL_VALUE == recordingSubscriptionId) {
            recordingSubscriptionId = aeronArchive.startRecording(channel, stream, SourceLocation.LOCAL);
        }

        return Aeron.NULL_VALUE != recordingSubscriptionId;
    }

    public boolean extendRecording(long recordingId, String channel, int stream) {
        if (Aeron.NULL_VALUE == recordingSubscriptionId) {
            recordingSubscriptionId = aeronArchive.extendRecording(recordingId, channel, stream, SourceLocation.LOCAL);
        }

        return  Aeron.NULL_VALUE != recordingSubscriptionId;
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

    public String liveEventStreamDestination(TransportParams params, String leaderHostName) {
        switch (params.connectUsing()) {
            case Unicast:
                return new ChannelUriStringBuilder()
                        .media(CommonContext.UDP_MEDIA)
                        .controlEndpoint(leaderHostName + ":" + EVENT_STREAM_CONTROL_PORT)
                        .endpoint(DYNAMIC_ENDPOINT)
                        .build();
            case Multicast:
                return new ChannelUriStringBuilder()
                        .media(CommonContext.UDP_MEDIA)
                        .endpoint(MULTICAST_EVENT_STREAM_ENDPOINT)
                        .build();
            default:
                throw new IllegalArgumentException("ConnectUsing not supported: " + params.connectUsing());
        }
    }
}
