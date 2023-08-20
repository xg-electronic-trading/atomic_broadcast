package utils;

import atomic_broadcast.listener.SequencerEventPrinter;
import atomic_broadcast.sequencer.SequencerCommandHandler;
import atomic_broadcast.utils.ConnectAs;
import atomic_broadcast.utils.ConnectUsing;
import atomic_broadcast.utils.EventReaderType;
import atomic_broadcast.utils.TransportParams;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.CommonContext;

import static atomic_broadcast.aeron.AeronModule.COMMAND_ENDPOINT;

public class TestTransportParams {

    public static TransportParams createSequencerParams() {
        return new TransportParams()
                .connectAs(ConnectAs.Sequencer)
                .connectUsing(ConnectUsing.Unicast)
                .addListener(new SequencerCommandHandler())
                .addListener(new SequencerEventPrinter())
                .instance(1);
    }

    public static TransportParams createConsensusParams() {
        return new TransportParams()
                .connectAs(ConnectAs.ClusterClient)
                .connectUsing(ConnectUsing.Unicast);
    }

    public static TransportParams createClientParams() {
        return new TransportParams()
                .connectAs(ConnectAs.Client)
                .connectUsing(ConnectUsing.Unicast)
                .withEventReader(EventReaderType.Direct)
                .addPublicationChannel(new ChannelUriStringBuilder()
                        .media(CommonContext.UDP_MEDIA)
                        .endpoint(COMMAND_ENDPOINT)
                        .build());
    }
}
