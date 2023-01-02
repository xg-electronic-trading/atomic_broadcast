package container;

import atomic_broadcast.aeron.AeronClient;
import atomic_broadcast.aeron.AeronParams;
import atomic_broadcast.aeron.AeronTransportClient;
import atomic_broadcast.client.EventBusTransportModule;
import atomic_broadcast.client.TransportClient;
import atomic_broadcast.utils.CompositeModule;
import atomic_broadcast.utils.ConnectAs;
import atomic_broadcast.utils.ConnectUsing;
import atomic_broadcast.utils.TransportParams;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import org.agrona.IoUtil;

public class AlgoContainerMain {

    private static final Log log = LogFactory.getLog(AlgoContainerMain.class.getName());

    public static void main(String[] args) {
        try {
            String aeronDir = IoUtil.tmpDirName()+"/"+"aeron";
            log.info().append("using aeron dir: ").appendLast(aeronDir);

            CompositeModule modules = new CompositeModule();
            TransportParams clientParams = new TransportParams();
            clientParams
                    .connectAs(ConnectAs.Client)
                    .connectUsing(ConnectUsing.Unicast);

            AeronParams params = new AeronParams()
                    .commandPort(40001)
                    .eventPort(40002)
                    .archivePort(8010)
                    .aeronDir(aeronDir)
                    .lowLatencyMode(false);

            AeronClient aeronClient = new AeronClient(params);
            TransportClient transportClient = new AeronTransportClient(aeronClient, clientParams);
            EventBusTransportModule eventbus = new EventBusTransportModule(transportClient, clientParams);
            modules.add(aeronClient);
            modules.add(eventbus);

            modules.start();


        } catch (Exception e) {
            log.error().append("error in AlgoContainerMain: ").appendLast(e);
        }
    }
}
