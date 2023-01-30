package container;

import atomic_broadcast.aeron.AeronClient;
import atomic_broadcast.aeron.AeronParams;
import atomic_broadcast.aeron.AeronTransportClient;
import atomic_broadcast.client.CommandValidator;
import atomic_broadcast.client.EventReaderModule;
import atomic_broadcast.client.TransportClient;
import atomic_broadcast.utils.*;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import events.AlgoEventListener;
import events.AlgoOrderEventHandler;
import events.OrderEventHandler;
import org.agrona.IoUtil;
import subscriptions.MarketDataHandler;
import subscriptions.MarketDataHandlerImpl;
import validator.AlgoCommandValidator;

import java.util.concurrent.atomic.AtomicBoolean;

public class AlgoContainerMain {

    private static final Log log = LogFactory.getLog(AlgoContainerMain.class.getName());

    public static void main(String[] args) {
        try {
            start(
                 new AlgoOrderEventHandler(),
                 new MarketDataHandlerImpl(),
                 new AlgoCommandValidator()
            );
        } catch (Exception e) {
            log.error().append("error in AlgoContainerMain: ").appendLast(e);
        }
    }

    public static void start(
            OrderEventHandler eventHandler,
            MarketDataHandler marketDataHandler,
            CommandValidator commandValidator
    ) {
        AtomicBoolean isReady = new AtomicBoolean();
        String aeronDir = IoUtil.tmpDirName()+"/"+"aeron";
        log.info().append("using aeron dir: ").appendLast(aeronDir);

        AlgoEventListener eventListener = new AlgoEventListener(eventHandler);

        CompositeModule modules = new CompositeModule();
        TransportParams clientParams = new TransportParams();
        clientParams
                .connectAs(ConnectAs.Client)
                .connectUsing(ConnectUsing.Unicast)
                .withEventReader(EventReaderType.RingBuffer);

        AeronParams params = new AeronParams()
                .commandPort(40001)
                .eventPort(40002)
                .archivePort(8010)
                .aeronDir(aeronDir)
                .lowLatencyMode(false);

        AeronClient aeronClient = new AeronClient(params);
        TransportClient transportClient = new AeronTransportClient(aeronClient, clientParams);
        EventReaderModule eventbus = new EventReaderModule(transportClient, clientParams, eventListener);
        modules.add(aeronClient);
        modules.add(eventbus);

        modules.start();

        Pollable mainLoop = new AlgoMainLoop(eventbus.eventsReader(), null);

        while (isReady.get()) {
            mainLoop.poll();
        }
    }
}
