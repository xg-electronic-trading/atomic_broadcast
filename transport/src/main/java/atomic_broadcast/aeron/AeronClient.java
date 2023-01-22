package atomic_broadcast.aeron;

import atomic_broadcast.utils.ConnectAs;
import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.ModuleName;
import atomic_broadcast.utils.TransportParams;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.codecs.RecordingSignal;
import io.aeron.archive.codecs.SourceLocation;
import org.agrona.CloseHelper;

import java.util.Comparator;
import java.util.Optional;

import static atomic_broadcast.aeron.AeronModule.CONTROL_RESPONSE_CHANNEL;
import static atomic_broadcast.utils.ModuleName.AeronClient;

public class AeronClient implements Module {

    Log log = LogFactory.getLog(this.getClass().getName());

    public static final String LOCAL_ENDPOINT = "aeron:udp?endpoint=localhost:";
    public static final String REMOTE_ENDPOINT = "aeron:udp?endpoint=somehost:";

    private final AeronParams params;
    private Aeron aeron;
    private AeronArchive.AsyncConnect asyncConnect;
    private AeronArchive aeronArchive;
    private AeronArchive srcAeronArchive; // this is the archive instance to replicate from.

    private long replicationSessionId = Aeron.NULL_VALUE;
    private long recordingSubscriptionId = Aeron.NULL_VALUE;

    private final RecordingDescriptorConsumerImpl recordingDescriptorConsumer = new RecordingDescriptorConsumerImpl();
    private final RecordingSignalConsumerImpl recordingSignalConsumer = new RecordingSignalConsumerImpl();
    private final RecordingDescriptor recordingDescriptor = new RecordingDescriptor();

    @Override
    public ModuleName name() {
        return AeronClient;
    }

    public AeronClient(AeronParams params) {
        this.params = params;
    }

    public void updatePrimaryEventSource(String host) {

    }


    @Override
    public void start() {
        aeron = Aeron.connect(
                new Aeron.Context()
                        .aeronDirectoryName(params.aeronDir()));

        log.info().appendLast("connected to media driver");
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
        String localArchiveChannel = LOCAL_ENDPOINT + params.archiveRequestPort();
        aeronArchive = connectToArchive(createNewArchiveCtx(localArchiveChannel));
        return null != aeronArchive;
    }

    private boolean connectToSrcArchive() {
        if(null != srcAeronArchive) {
            return true;
        }
        String remoteArchiveChannel = LOCAL_ENDPOINT + params.archiveRequestPort();
        srcAeronArchive = connectToArchive(createNewArchiveCtx(remoteArchiveChannel));
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

    public boolean startReplication(TransportParams transportParams, RecordingDescriptor dstRecordingId) {
        if (transportParams.connectAs() == ConnectAs.Client) {
            throw new UnsupportedOperationException("client application cannot perform replication.");
        } else {
            boolean isConnectedToSrcArchive = connectToSrcArchive();
            if (isConnectedToSrcArchive) {
                RecordingDescriptor srcRecording = findRecording(srcAeronArchive);
                replicationSessionId = aeronArchive.replicate(
                        srcRecording.recordingId(),
                        dstRecordingId.recordingId(),
                        AeronArchive.Configuration.CONTROL_STREAM_ID_DEFAULT,
                        REMOTE_ENDPOINT + params.archiveRequestPort(),
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
