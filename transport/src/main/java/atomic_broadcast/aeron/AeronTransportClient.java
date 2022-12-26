package atomic_broadcast.aeron;

import atomic_broadcast.client.TransportClient;
import atomic_broadcast.utils.TransportParams;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import io.aeron.*;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.client.ReplayMerge;
import io.aeron.logbuffer.BufferClaim;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.DirectBuffer;

import static atomic_broadcast.aeron.AeronModule.*;
import static io.aeron.Publication.*;
import static io.aeron.archive.client.RecordingSignalPoller.FRAGMENT_LIMIT;

public class AeronTransportClient implements TransportClient {

    private static final Log log = LogFactory.getLog(AeronTransportClient.class.getName());

    AeronClient aeronClient;
    TransportParams params;

    private RecordingDescriptor latestRecording;
    private Publication publication;
    private Subscription subscription;
    private ReplayMerge replayMerge;
    private FragmentHandler fragmentHandler;

    private final BufferClaim bufferClaim = new BufferClaim();

    private final String commandStreamPublicationChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .endpoint(COMMAND_ENDPOINT)
            .build();

    ChannelUriStringBuilder udpSubscriptionChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .controlMode(CommonContext.MDC_CONTROL_MODE_MANUAL);

    ChannelUriStringBuilder udpReplayChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA);

    private final String udpReplayDestinationChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .endpoint(DYNAMIC_ENDPOINT)
            .build();

    public AeronTransportClient(AeronClient aeronClient, TransportParams params) {
        this.aeronClient = aeronClient;
        this.params = params;
        this.fragmentHandler = new FragmentAssembler(new AeronClientFragmentHandler(params.listeners()));
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
        try {
            switch (params.connectUsing()) {
                case Unicast:
                    String subscriptionChannel = udpSubscriptionChannel
                            .sessionId(latestRecording.sessionId())
                            .build();

                    String replayChannel = udpReplayChannel
                            .sessionId(latestRecording.sessionId())
                            .build();

                    String liveDestination = new ChannelUriStringBuilder()
                            .media(CommonContext.UDP_MEDIA)
                            .controlEndpoint(CONTROL_ENDPOINT)
                            .endpoint(DYNAMIC_ENDPOINT)
                            .build();

                    replayMerge = replayMerge(
                            latestRecording.recordingId(),
                            subscriptionChannel,
                            replayChannel,
                            udpReplayDestinationChannel,
                            liveDestination,
                            aeronClient.aeronArchive()
                    );

                    return true;

                case Multicast:
                case Ipc:
                case Journal:
            }
        } catch (Exception e) {
            log.error().append("exception whilst trying to connect to event stream: ").appendLast(e);
            return false;
        }

        return false;
    }

    @Override
    public boolean pollReplay() {
        if (replayMerge != null) {
            if (!replayMerge.isMerged()) {
                replayMerge.poll(fragmentHandler, FRAGMENT_LIMIT);
                return false;
            } else {
                log.info().appendLast(replayMerge);
                replayMerge = null;
                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    public boolean pollEventStream() {
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

    @Override
    public void close() throws Exception {
        aeronClient.closePublication(publication);
        aeronClient.closeSubscription(subscription);
        publication = null;
        subscription = null;
    }

    @Override
    public boolean connectToCommandStream() {
        if (null == publication) {
            publication = aeronClient.addPublication(commandStreamPublicationChannel, COMMAND_STREAM_ID);
            return null != publication;
        } else {
            return true;
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
    public boolean send(DirectBuffer buffer, int offset, int length) {
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

        return true;
    }
}
