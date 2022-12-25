/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package atomic_broadcast;

import atomic_broadcast.aeron.AeronModule;
import atomic_broadcast.aeron.AeronSequencerClient;
import atomic_broadcast.aeron.AeronTransportClient;
import atomic_broadcast.client.TransportClient;
import atomic_broadcast.consensus.SeqNoClient;
import atomic_broadcast.consensus.SeqNoProvider;
import atomic_broadcast.consensus.ShmSeqNoClient;
import atomic_broadcast.consensus.ShmSeqNoServer;
import atomic_broadcast.sequencer.*;
import atomic_broadcast.utils.CompositeModule;
import atomic_broadcast.utils.ConnectAs;
import atomic_broadcast.utils.ConnectUsing;
import atomic_broadcast.utils.TransportParams;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import static atomic_broadcast.utils.TransportState.PollCommandStream;
import static atomic_broadcast.utils.TransportState.PollEventStream;

public class AllAppsMain {

    private static final Log log = LogFactory.getLog(AllAppsMain.class.getName());

    public static void main(String[] args) {
        try {
            CompositeModule modules = new CompositeModule();

            ShmSeqNoServer shmSeqNoServer = new ShmSeqNoServer();
            shmSeqNoServer.setReady(true);
            shmSeqNoServer.writeSeqNum(1, 1, 0);

            TransportParams clientParams = new TransportParams();
            TransportParams sequencerParams = new TransportParams();

            clientParams.connectAs(ConnectAs.Client).connectUsing(ConnectUsing.Unicast);
            sequencerParams
                    .connectAs(ConnectAs.Sequencer)
                    .connectUsing(ConnectUsing.Unicast)
                    .addListener(new SequencerCommandHandler())
                    .instance(1);

            AeronModule aeronModule = new AeronModule(true, true, false);

            SequencerClient sequencerClient = new AeronSequencerClient(aeronModule, sequencerParams);
            SeqNoProvider seqNoProvider = new SeqNoClient(new ShmSeqNoClient());
            SequencerModule sequencerModule = new SequencerModule(sequencerParams, sequencerClient, seqNoProvider);

            TransportClient transportClient = new AeronTransportClient(aeronModule, clientParams);
            EventBusTransportModule transportModule = new EventBusTransportModule(transportClient, clientParams);

            modules.add(aeronModule);
            modules.add(sequencerModule);
            modules.add(transportModule);

            modules.start();

            while (sequencerModule.state() != PollCommandStream) {
                modules.poll();
            }

            while(transportModule.state() != PollEventStream) {
                modules.poll();
            }


            log.info().append("seq publication connected: ").appendLast(sequencerClient.isPublicationConnected());
            log.info().append("client subscription connected: ").appendLast(transportClient.isSubscriptionConnected());

            modules.close();
        } catch (Exception e) {
           log.error().append("error in AllAppsMain: ").appendLast(e);
        }
    }
}
