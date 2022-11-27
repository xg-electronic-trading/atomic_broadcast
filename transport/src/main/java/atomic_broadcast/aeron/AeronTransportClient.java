package atomic_broadcast.aeron;

import atomic_broadcast.client.TransportClient;
import atomic_broadcast.utils.TransportParams;
import io.aeron.*;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.client.ReplayMerge;
import io.aeron.logbuffer.FragmentHandler;

import static io.aeron.archive.client.RecordingSignalPoller.FRAGMENT_LIMIT;

public class AeronTransportClient implements TransportClient {

    AeronModule aeronModule;
    TransportParams params;

    private RecordingDescriptor latestRecording;
    private Subscription subscription;
    private ReplayMerge replayMerge;
    private FragmentHandler fragmentHandler;


    private static final String DYNAMIC_ENDPOINT = "localhost:0";
    private static final String CONTROL_ENDPOINT = "localhost:23265";
    private static final int EVENT_STREAM_ID = 10_000_001;

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

        return false;
    }

    @Override
    public boolean pollEventStream() {
        if (replayMerge != null) {
            if (!replayMerge.isMerged()) {
                replayMerge.poll(fragmentHandler, FRAGMENT_LIMIT);
            } else {
                replayMerge = null;
            }
        } else {
            subscription.poll(fragmentHandler, FRAGMENT_LIMIT);
        }
        return true;
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
}
