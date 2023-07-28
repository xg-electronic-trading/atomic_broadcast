package atomic_broadcast.aeron;

import atomic_broadcast.consensus.ClientSeqNumWriter;
import atomic_broadcast.consensus.ClusterMember;
import atomic_broadcast.consensus.ConsensusStateHolder;
import atomic_broadcast.sequencer.SequencerClient;
import atomic_broadcast.utils.InstanceInfo;
import atomic_broadcast.utils.ReplayState;
import atomic_broadcast.utils.TransportParams;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import io.aeron.*;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.client.ReplayMerge;
import io.aeron.archive.codecs.RecordingSignal;
import io.aeron.driver.Configuration;
import io.aeron.logbuffer.BufferClaim;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.DirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;
import time.Clock;

import java.util.PrimitiveIterator;

import static atomic_broadcast.aeron.AeronModule.*;
import static atomic_broadcast.utils.ReplayState.*;
import static io.aeron.Publication.*;
import static io.aeron.archive.client.RecordingSignalPoller.FRAGMENT_LIMIT;

public class AeronSequencerClient implements SequencerClient {

    private static final Log log = LogFactory.getLog(AeronSequencerClient.class.getName());

    private static final int PUBLICATION_TAG = 2;

    private final InstanceInfo instanceInfo;
    private final Clock clock;
    private final AeronClient aeronClient;
    private final TransportParams params;
    private final ConsensusStateHolder consensusState;
    private RecordingDescriptor latestRecording;
    private Subscription subscription;
    private Publication publication;
    private FragmentHandler fragmentHandler;
    private Long2ObjectHashMap<ClusterMember> clusterMembers;
    private long position = -1;
    private final BufferClaim bufferClaim = new BufferClaim();
    private long addPublicationDelay = Aeron.NULL_VALUE;

    private final String commandStreamSubscriptionChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .endpoint(COMMAND_ENDPOINT)
            .build();

    private String publicationChannel;
    private ChannelUriStringBuilder extendPublicationBuilder;

    ChannelUriStringBuilder manualSubscriptionChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .controlMode(CommonContext.MDC_CONTROL_MODE_MANUAL);

    ChannelUriStringBuilder udpReplayChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .linger(0L)
            .eos(false);

    private final String udpReplayDestinationChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .endpoint(DYNAMIC_ENDPOINT)
            .build();

    public AeronSequencerClient(InstanceInfo instanceInfo,
                                AeronClient aeronClient,
                                TransportParams params,
                                ConsensusStateHolder consensusState,
                                ClientSeqNumWriter seqNumWriter,
                                Long2ObjectHashMap<ClusterMember> clusterMembers,
                                Clock clock
                                ) {
        this.instanceInfo = instanceInfo;
        this.aeronClient = aeronClient;
        this.params = params;
        this.consensusState = consensusState;
        this.fragmentHandler = new FragmentAssembler(new AeronSequencerFragmentHandler(this, params.listeners(), seqNumWriter, params.instanceId()));
        this.clusterMembers = clusterMembers;
        this.clock = clock;
        createEventStreamPublicationChannel();
        createEventStreamExtendPublicationChannel();
    }

    private void createEventStreamPublicationChannel() {
         publicationChannel = new ChannelUriStringBuilder()
                .media(CommonContext.UDP_MEDIA)
                .controlEndpoint(instanceInfo.hostname() + ":" + EVENT_STREAM_CONTROL_PORT) //change this to endpoint and remove control mode when using multicast
                .controlMode(CommonContext.MDC_CONTROL_MODE_DYNAMIC)
                .build();
    }

    private void createEventStreamExtendPublicationChannel() {
        extendPublicationBuilder = new ChannelUriStringBuilder()
                .media(CommonContext.UDP_MEDIA)
                .controlEndpoint(instanceInfo.hostname() + ":" + EVENT_STREAM_CONTROL_PORT) //change this to endpoint and remove control mode when using multicast
                .controlMode(CommonContext.MDC_CONTROL_MODE_DYNAMIC);
    }

    @Override
    public boolean connectToJournalSource() {
        return aeronClient.connectToArchive();
    }

    @Override
    public boolean findJournal() {
        latestRecording = aeronClient.findRecording();
        boolean journalFound = latestRecording.recordingId() != Aeron.NULL_VALUE;

        if (journalFound) {
            log.info().append("app: ").append(instanceInfo.app())
                    .append(", instance: ").append(instanceInfo.instance())
                    .append(", recording: ").appendLast(latestRecording);
        }

        return journalFound;
    }

    @Override
    public boolean connectToEventStream() {
        return false;
    }

    public ReplayState startReplay() {
        switch (params.connectUsing()) {
            case Multicast:
            case Unicast:
                latestRecording = aeronClient.findRecording();
                boolean recordingAvailable = Aeron.NULL_VALUE != latestRecording.recordingId();
                return startFixedReplay(recordingAvailable);
            case Ipc:
            case Journal:
                return NotStarted;
        }

        return NotStarted;
    }

    public boolean startTailEventJournal() {
        switch (params.connectUsing()) {
            case Multicast:
            case Unicast:
                latestRecording = aeronClient.findActiveRecording();
                boolean recordingAvailable = Aeron.NULL_VALUE != latestRecording.recordingId();
                return startOpenEndedReplay(recordingAvailable);
            case Ipc:
            case Journal:
                return false;
        }

        return false;
    }

    private boolean startOpenEndedReplay(boolean isRecordingAvailable) {
        boolean started = false;
        if (isRecordingAvailable) {
            String replayChannel = addReplaySubscription();
            if (null != replayChannel) {
                started = aeronClient.startOpenEndedReplay(latestRecording, replayChannel);
            }
        }

        return started;
    }

    private ReplayState startFixedReplay(boolean recordingAvailable) {
        ReplayState state = NotStarted;
        boolean skipReplay = startGreaterThanStopPosition(latestRecording.startPosition(), latestRecording.stopPosition());

        if (skipReplay) {
            log.info().append("app: ").append(instanceInfo.app())
                    .append(", instance: ").append(instanceInfo.instance())
                    .appendLast(", skipping replay as startPosition >= stopPosition");
            state = Skipped;
        } else {
            if (recordingAvailable) {
                String replayChannel = addReplaySubscription();
                if (null != replayChannel) {
                    if (aeronClient.startReplay(latestRecording, replayChannel)) {
                        state = Started;
                    };
                }
            }
        }
        return state;
    }

    /**
     *
     * @return - return replay channel for replay subscription.
     */
    private String addReplaySubscription() {
        if (null == subscription) {
            String subscriptionChannel = manualSubscriptionChannel
                    .sessionId(latestRecording.sessionId())
                    .build();
            subscription = aeronClient.addSubscription(subscriptionChannel, EVENT_STREAM_ID);
            subscription.asyncAddDestination(udpReplayDestinationChannel);
        }

        String resolvedEndpoint = subscription.resolvedEndpoint();

        boolean replayInitialised = null != resolvedEndpoint;

        String replayChannel = null;

        if (replayInitialised) {
            replayChannel = udpReplayChannel
                    .sessionId(latestRecording.sessionId())
                    .endpoint(resolvedEndpoint)
                    .build();
        }

        return replayChannel;
    }

    @Override
    public boolean pollEventStream() {
        return false;
    }

    @Override
    public boolean pollReplay() {
        boolean isDone = false;

        if (null != subscription) {
            subscription.poll(fragmentHandler, FRAGMENT_LIMIT);
            int sessionId = (int) aeronClient.replaySessionId();
            Image image = subscription.imageBySessionId(sessionId);

            if (null != image) {
                isDone = image.position() >= latestRecording.stopPosition();
            }
        }

        if (isDone) {
            aeronClient.closeReplay();
        }

        return isDone;
    }

    @Override
    public boolean pollJournal() {
        boolean isStopped = aeronClient.pollForRecordingSignal(RecordingSignal.STOP);
        if (isStopped) {
            subscription.close();
            subscription = null;
            return false;
        } else {
            subscription.poll(fragmentHandler, FRAGMENT_LIMIT);
            return true;
        }
    }

    @Override
    public boolean isSubscriptionConnected() {
        return subscription.isConnected();
    }

    @Override
    public boolean isSubscriptionClosed() {
        return subscription.isClosed();
    }

    @Override
    public boolean connectToCommandStream() {
        if (null == subscription) {
            subscription = aeronClient.addSubscription(commandStreamSubscriptionChannel, COMMAND_STREAM_ID);
            return subscription != null;
        } else {
            return true;
        }
    }

    @Override
    public boolean pollCommandStream() {
        subscription.poll(fragmentHandler, 1000);
        return true;
    }

    @Override
    public boolean isReplayActive() {
        return Aeron.NULL_VALUE != aeronClient.replaySessionId() && subscription.isConnected();
    }

    @Override
    public boolean startReplication() {
        ClusterMember leader = clusterMembers.get(consensusState.getLeaderInstance());
        boolean started = aeronClient.startReplication(params, latestRecording, leader);
        if (started) {
            return aeronClient.checkReplicationDone();
        } else {
            return false;
        }
    }

    @Override
    public boolean stopReplication() {
        aeronClient.closeReplication();
        return true;
    }

    private boolean delayAddPublication() {
        if (addPublicationDelay != Aeron.NULL_VALUE) {
            return clock.nanoTime() <= addPublicationDelay;
        }

        addPublicationDelay = clock.nanoTime() + Configuration.publicationLingerTimeoutNs();
        return true;
    }

    @Override
    public boolean createEventStream() {
        boolean delayPublication = delayAddPublication();
        if (delayPublication) {
            return false;
        } else {
            if (null == publication) {
                publication = aeronClient.addExclusivePublication(publicationChannel, EVENT_STREAM_ID);
                log.info().append("app: ").append(instanceInfo.app())
                        .append(", instance: ").append(instanceInfo.instance())
                        .append(", publication session-id: ").appendLast(publication.sessionId());
                return publication != null;
            } else {
                return true;
            }
        }
    }

    @Override
    public boolean extendEventStream() {
        boolean delayPublication = delayAddPublication();
        if (delayPublication) {
            return false;
        } else {
            if (null == publication) {
                boolean journalFound = findJournal();
                if (journalFound) {
                    String extendPublicationChannel = extendPublicationBuilder
                            .initialPosition(latestRecording.stopPosition(), latestRecording.initialTermId(), latestRecording.termBufferLength())
                            .mtu(latestRecording.mtuLength())
                            .build();
                    publication = aeronClient.addExclusivePublication(extendPublicationChannel, EVENT_STREAM_ID);
                    log.info().append("app: ").append(instanceInfo.app())
                            .append(", instance: ").append(instanceInfo.instance())
                            .append(", extended publication session-id: ").appendLast(publication.sessionId());
                    return publication != null;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
    }

    @Override
    public boolean createEventJournal() {
        boolean isCreated = aeronClient.startRecording(publicationChannel, EVENT_STREAM_ID);
        if (isCreated) {
            return aeronClient.pollForRecordingSignal(RecordingSignal.START);
        } else {
            return false;
        }

    }

    @Override
    public boolean extendEventJournal() {
        boolean isExtended = aeronClient.extendRecording(latestRecording.recordingId(), publicationChannel, EVENT_STREAM_ID);
        if (isExtended) {
            return aeronClient.pollForRecordingSignal(RecordingSignal.EXTEND);
        } else {
            return false;
        }
    }

    @Override
    public boolean isPublicationConnected() {
        return publication.isConnected();
    }

    @Override
    public boolean isPublicationClosed() {
        return publication.isClosed();
    }

    @Override
    public boolean publish(DirectBuffer buffer, int offset, int length) {
        if (null != publication) {
            if (length > publication.maxPayloadLength()) {
                /**
                 * send messages > MTU using standard offer().
                 * these will be fragmented over the wire.
                 */
                long result = publication.offer(buffer, offset, length);
                return processResult(result);
            } else {
                /**
                 * send messages <= MTU via tryClaim() using
                 * zero copy semantics for increased performance.
                 */
                long result = publication.tryClaim(length, bufferClaim);
                if (result > 0) {
                    bufferClaim.putBytes(buffer, offset, length);
                    bufferClaim.commit();
                } else {
                    bufferClaim.abort();
                }
                return processResult(result);
            }
        } else {
            return false;
        }
    }

    @Override
    public long position() {
        return position;
    }

    private boolean processResult(long publicationResult) {
        if (publicationResult == NOT_CONNECTED) {
            log.error().appendLast("publication not connected.");
            return false;
        } else if (publicationResult == BACK_PRESSURED) {
            log.error().appendLast("publication back pressured. please retry offer.");
            return false;
        } else if (publicationResult == ADMIN_ACTION) {
            log.error().appendLast("publication admin action. please retry offer.");
            return false;
        } else if (publicationResult == CLOSED) {
            log.error().appendLast("publication closed. cannot offer.");
            return false;
        } else if (publicationResult == MAX_POSITION_EXCEEDED) {
            log.error().appendLast("max position exceeded. publication should be closed and then a new one added.");
            return false;
        }

        /**
         * update position once message has been sent.
         */
        position = publication.position();

        return true;
    }

    private boolean startGreaterThanStopPosition(long startPosition, long stopPosition) {
        return startPosition >= stopPosition;
    }

    @Override
    public void close() {
        aeronClient.closeReplay();
        aeronClient.closeSubscription(subscription);
        aeronClient.closeReplication();
        aeronClient.closeRecording();
        aeronClient.closePublication(publication);
        subscription = null;
        publication = null;
        addPublicationDelay = Aeron.NULL_VALUE;
    }
}
