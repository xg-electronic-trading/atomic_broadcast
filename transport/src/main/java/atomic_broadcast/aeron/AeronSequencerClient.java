package atomic_broadcast.aeron;

import atomic_broadcast.consensus.ClientSeqNumWriter;
import atomic_broadcast.consensus.ClusterMember;
import atomic_broadcast.consensus.ConsensusStateHolder;
import atomic_broadcast.sequencer.SequencerClient;
import atomic_broadcast.utils.InstanceInfo;
import atomic_broadcast.utils.TransportParams;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import io.aeron.*;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.client.ReplayMerge;
import io.aeron.archive.codecs.RecordingSignal;
import io.aeron.logbuffer.BufferClaim;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.DirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;

import static atomic_broadcast.aeron.AeronModule.*;
import static io.aeron.Publication.*;
import static io.aeron.archive.client.RecordingSignalPoller.FRAGMENT_LIMIT;

public class AeronSequencerClient implements SequencerClient {

    private static final Log log = LogFactory.getLog(AeronSequencerClient.class.getName());

    private static final int PUBLICATION_TAG = 2;

    private final InstanceInfo instanceInfo;
    private final AeronClient aeronClient;
    private final TransportParams params;
    private final ConsensusStateHolder consensusState;
    private RecordingDescriptor latestRecording;
    private Subscription subscription;
    private Publication publication;
    private ReplayMerge replayMerge;
    private FragmentHandler fragmentHandler;
    private Long2ObjectHashMap<ClusterMember> clusterMembers;
    private long position = -1;
    private final BufferClaim bufferClaim = new BufferClaim();

    private final String commandStreamSubscriptionChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .endpoint(COMMAND_ENDPOINT)
            .build();

    private String publicationChannel;

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
                                Long2ObjectHashMap<ClusterMember> clusterMembers
                                ) {
        this.instanceInfo = instanceInfo;
        this.aeronClient = aeronClient;
        this.params = params;
        this.consensusState = consensusState;
        this.fragmentHandler = new FragmentAssembler(new AeronSequencerFragmentHandler(this, params.listeners(), seqNumWriter, params.instanceId()));
        this.clusterMembers = clusterMembers;
        createEventStreamPublicationChannel();
    }

    private void createEventStreamPublicationChannel() {
         publicationChannel = new ChannelUriStringBuilder()
                .media(CommonContext.UDP_MEDIA)
                .controlEndpoint(instanceInfo.hostname() + ":" + EVENT_STREAM_CONTROL_PORT) //change this to endpoint and remove control mode when using multicast
                .controlMode(CommonContext.MDC_CONTROL_MODE_DYNAMIC)
                .build();
    }

    @Override
    public boolean connectToJournalSource() {
        return aeronClient.connectToArchive();
    }

    @Override
    public boolean findJournal() {
        latestRecording = aeronClient.findRecording();
        return latestRecording.recordingId() != Aeron.NULL_VALUE;
    }

    @Override
    public boolean connectToEventStream() {
        switch (params.connectUsing()) {
            case Multicast:
            case Unicast:
                latestRecording = aeronClient.findActiveRecording();
                if (latestRecording.recordingId() != Aeron.NULL_VALUE) {
                    if (null == subscription) {
                        String subscriptionChannel = manualSubscriptionChannel
                                .sessionId(latestRecording.sessionId())
                                .build();
                        subscription = aeronClient.addSubscription(subscriptionChannel, EVENT_STREAM_ID);
                        subscription.asyncAddDestination(udpReplayDestinationChannel);
                    }

                    String resolvedEndpoint = subscription.resolvedEndpoint();

                    if (null != resolvedEndpoint) {
                        String replayChannel = udpReplayChannel
                                .endpoint(resolvedEndpoint)
                                .build();
                        return aeronClient.startOpenEndedReplay(latestRecording, replayChannel);
                    }
                }
                break;
            case Ipc:
            case Journal:
                return false;
        }

        return false;
    }

    @Override
    public boolean pollEventStream() {
        return false;
    }

    @Override
    public boolean pollReplay() {
        aeronClient.checkReplicationDone();
        subscription.poll(fragmentHandler, FRAGMENT_LIMIT);
        return true;
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
    public boolean startReplication() {
        ClusterMember leader = clusterMembers.get(consensusState.getLeaderInstance());
        boolean started = aeronClient.startReplication(params, latestRecording, leader);
        if (started) {
            return aeronClient.pollForRecordingSignal(RecordingSignal.MERGE);
        } else {
            return false;
        }
    }

    @Override
    public boolean stopReplication() {
        aeronClient.closeReplication();
        return true;
    }

    @Override
    public boolean createEventStream() {
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

    private ReplayMerge replayMerge(long recordingId,
                                    String subscriptionChannel,
                                    String replayChannel,
                                    String replayDestination,
                                    String liveDestination,
                                    AeronArchive aeronArchive) {
        subscription = aeronClient.addSubscription(subscriptionChannel, EVENT_STREAM_ID);

        return new ReplayMerge(
                subscription,
                aeronArchive,
                replayChannel,
                replayDestination,
                liveDestination,
                recordingId,
                0
        );
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

    @Override
    public void close() {
        aeronClient.closeReplay();
        aeronClient.closeSubscription(subscription);
        aeronClient.closeReplication();
        aeronClient.closeRecording();
        aeronClient.closePublication(publication);
        subscription = null;
        publication = null;
    }
}
