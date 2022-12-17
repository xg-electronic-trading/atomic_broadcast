package atomic_broadcast.aeron;

import atomic_broadcast.sequencer.SequencerClient;
import atomic_broadcast.utils.TransportParams;
import io.aeron.*;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.client.ReplayMerge;
import io.aeron.logbuffer.FragmentHandler;

import static atomic_broadcast.aeron.AeronModule.*;

public class AeronSequencerClient implements SequencerClient {

    private static final int PUBLICATION_TAG = 2;

    private final AeronModule aeronModule;
    private final TransportParams params;
    private RecordingDescriptor latestRecording;
    private Subscription subscription;
    private Publication publication;
    private ReplayMerge replayMerge;
    private FragmentHandler fragmentHandler;

    private final String commandStreamSubscriptionChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .endpoint(COMMAND_ENDPOINT)
            .build();

    private final String publicationChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .tags("1," + PUBLICATION_TAG)
            .controlEndpoint(CONTROL_ENDPOINT) //change this to endpoint and remove control mode when using multicast
            .controlMode(CommonContext.MDC_CONTROL_MODE_DYNAMIC)
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

    public AeronSequencerClient(AeronModule aeronModule, TransportParams params) {
        this.aeronModule = aeronModule;
        this.params = params;
        this.fragmentHandler = new FragmentAssembler(new AeronSequencerFragmentHandler(params.listeners()));
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

        return true;
    }

    @Override
    public boolean pollEventStream() {
        return false;
    }

    @Override
    public boolean isSubscriptionConnected() {
        return subscription.isConnected();
    }

    @Override
    public boolean connectToCommandStream() {
        if (null == subscription) {
            subscription = aeronModule.addSubscription(commandStreamSubscriptionChannel, COMMAND_STREAM_ID);
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
        return aeronModule.startReplication(params, latestRecording);
    }

    @Override
    public boolean stopReplication() {
        aeronModule.closeReplication();
        return true;
    }

    @Override
    public boolean createEventStream() {
        if (null == publication) {
            publication = aeronModule.addPublication(publicationChannel, EVENT_STREAM_ID);
            return publication != null;
        } else {
            return true;
        }
    }

    @Override
    public boolean createEventJournal() {
        return aeronModule.startRecording(publicationChannel, EVENT_STREAM_ID);
    }

    @Override
    public boolean isPublicationConnected() {
        return publication.isConnected();
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
    public void close() {
        aeronModule.closeSubscription(subscription);
        aeronModule.closeReplication();
        aeronModule.closeRecording();
        aeronModule.closePublication(publication);
    }
}
