package atomic_broadcast.aeron;

import atomic_broadcast.client.TransportClient;
import atomic_broadcast.utils.TransportParams;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import io.aeron.*;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.client.ReplayMerge;
import io.aeron.logbuffer.FragmentHandler;

import static atomic_broadcast.aeron.AeronModule.*;
import static io.aeron.archive.client.RecordingSignalPoller.FRAGMENT_LIMIT;

public class AeronTransportClient implements TransportClient {

    private static final Log log = LogFactory.getLog(AeronTransportClient.class.getName());

    AeronModule aeronModule;
    TransportParams params;

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

    public AeronTransportClient(AeronModule aeronModule, TransportParams params) {
        this.aeronModule = aeronModule;
        this.params = params;
        this.fragmentHandler = new FragmentAssembler(new AeronClientFragmentHandler(params.listeners()));
    }

    @Override
    public boolean connectToJournalSource() {
        return aeronModule.connectToArchive();
    }

    @Override
    public boolean findJournal() {
        latestRecording = aeronModule.findRecording();
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
                            aeronModule.aeronArchive()
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
        subscription = aeronModule.addSubscription(subscriptionChannel, EVENT_STREAM_ID);

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
        aeronModule.closeSubscription(subscription);
    }
}
