package atomic_broadcast.aeron;

import atomic_broadcast.client.TransportClient;
import atomic_broadcast.utils.InstanceInfo;
import atomic_broadcast.utils.JournalState;
import atomic_broadcast.utils.TransportParams;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import io.aeron.*;
import io.aeron.archive.Archive;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.client.ArchiveException;
import io.aeron.archive.client.ReplayMerge;
import io.aeron.logbuffer.FragmentHandler;

import static atomic_broadcast.aeron.AeronModule.*;
import static atomic_broadcast.utils.JournalState.*;
import static io.aeron.archive.client.RecordingSignalPoller.FRAGMENT_LIMIT;

public class AeronTransportClient implements TransportClient {

    private static final Log log = LogFactory.getLog(AeronTransportClient.class.getName());

    private final AeronClient aeronClient;
    private final TransportParams params;
    private final InstanceInfo instanceInfo;

    private RecordingDescriptor latestRecording;
    private Subscription subscription;
    private ReplayMerge replayMerge;
    private FragmentHandler fragmentHandler;

    ChannelUriStringBuilder udpSubscriptionChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .controlMode(CommonContext.MDC_CONTROL_MODE_MANUAL);

    ChannelUriStringBuilder udpReplayChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA);

    private final String udpReplayDestinationChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .endpoint(DYNAMIC_ENDPOINT)
            .build();

    public AeronTransportClient(AeronClient aeronClient, TransportParams params, InstanceInfo instanceInfo) {
        this.aeronClient = aeronClient;
        this.params = params;
        this.instanceInfo = instanceInfo;
        this.fragmentHandler = new FragmentAssembler(new AeronClientFragmentHandler(params.listeners()));
    }

    @Override
    public boolean connectToJournalSource() {
        return aeronClient.connectToArchive();
    }

    @Override
    public JournalState findJournal() {
        JournalState journalState = JournalNotFound;
        latestRecording = aeronClient.findActiveRecording();
        boolean journalFound = latestRecording.recordingId() != Aeron.NULL_VALUE;

        if (journalFound) {
            journalState = ActiveJournal;
        }

        return journalState;
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
                            /**
                             * for MDC, we need to know the host that owns the event stream pub.
                             * This can potentially come from the sourceIdentity on the
                             * recording descriptor.
                             * For multicast, this is not a problem.
                             */
                            .controlEndpoint(LOCAL_HOST + ":" + EVENT_STREAM_CONTROL_PORT)
                            .endpoint(DYNAMIC_ENDPOINT)
                            .build();

                    log.info().append("app: ").append(instanceInfo.app())
                            .append(", instance: ").append(instanceInfo.instance())
                            .append(", recording to replay-merge: ").appendLast(latestRecording);

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
    public boolean startTailEventJournal() {
        return false;
    }

    @Override
    public boolean pollReplay() {
        if (replayMerge != null) {
            if (!replayMerge.isMerged()) {
                try {
                    replayMerge.poll(fragmentHandler, FRAGMENT_LIMIT);
                } catch (ArchiveException e) {
                    log.error().append("app: ").append(instanceInfo.app())
                            .append(", instance: ").append(instanceInfo.instance())
                            .append(", exception: ").appendLast(e);
                }
                return false;
            } else {
                log.info().appendLast(replayMerge.toString());
                replayMerge = null;
                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    public boolean pollJournal() {
        return false;
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
        aeronClient.closeSubscription(subscription);
        subscription = null;
    }
}
