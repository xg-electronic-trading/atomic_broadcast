package utils;

import atomic_broadcast.sequencer.SequencerCommandHandler;
import atomic_broadcast.utils.ConnectAs;
import atomic_broadcast.utils.ConnectUsing;
import atomic_broadcast.utils.TransportParams;

public class TestTransportParams {

    public static TransportParams createSequenceParams() {
        return new TransportParams()
                .connectAs(ConnectAs.Sequencer)
                .connectUsing(ConnectUsing.Unicast)
                .addListener(new SequencerCommandHandler())
                .instance(1);
    }

    public static TransportParams createClientParams() {
        return new TransportParams()
                .connectAs(ConnectAs.Client)
                .connectUsing(ConnectUsing.Unicast);
    }
}
