package utils;

import atomic_broadcast.sequencer.SequencerCommandHandler;
import atomic_broadcast.utils.ConnectAs;
import atomic_broadcast.utils.ConnectUsing;
import atomic_broadcast.utils.EventReaderType;
import atomic_broadcast.utils.TransportParams;
import listener.EventPrinter;

public class TestTransportParams {

    public static TransportParams createSequencerParams() {
        return new TransportParams()
                .connectAs(ConnectAs.Sequencer)
                .connectUsing(ConnectUsing.Unicast)
                .addListener(new SequencerCommandHandler())
                .instance(1);
    }

    public static TransportParams createClientParams() {
        return new TransportParams()
                .connectAs(ConnectAs.Client)
                .connectUsing(ConnectUsing.Unicast)
                .withEventReader(EventReaderType.Direct);
    }
}
