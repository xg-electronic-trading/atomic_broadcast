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
import orderstate.ByteBufferOrderStateCache;
import orderstate.OrderStateField;
import orderstate.StateCache;
import org.agrona.IoUtil;
import subscriptions.MarketDataHandler;
import subscriptions.MarketDataHandlerImpl;
import validator.AlgoCommandValidator;

import java.util.concurrent.atomic.AtomicBoolean;

import static atomic_broadcast.aeron.AeronModule.ARCHIVE_REQUEST_PORT_RANGE_START;
import static atomic_broadcast.utils.App.AlgoContainer;

public class AlgoContainerMain {

    private static final Log log = LogFactory.getLog(AlgoContainerMain.class.getName());

    public static void main(String[] args) {
        try {
            OrderStateField[] fields = new OrderStateField[]{
                    OrderStateField.Id,
                    OrderStateField.Price,
                    OrderStateField.Quantity,
                    OrderStateField.MsgSeqNum};

            StateCache cache = new ByteBufferOrderStateCache(false, 1, fields);

            start(
                 new AlgoOrderEventHandler(cache),
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
        InstanceInfo instanceInfo = new InstanceInfo(AlgoContainer, "localhost", 1);
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
                .archivePort(ARCHIVE_REQUEST_PORT_RANGE_START)
                .aeronDir(aeronDir)
                .lowLatencyMode(false);

        AeronClient aeronClient = new AeronClient(params, instanceInfo);
        TransportClient transportClient = new AeronTransportClient(aeronClient, clientParams);
        EventReaderModule eventbus = new EventReaderModule(transportClient, clientParams, eventListener, instanceInfo);
        modules.add(aeronClient);
        modules.add(eventbus);

        modules.start();

        Pollable mainLoop = new AlgoMainLoop(eventbus.eventsReader(), null);

        while (isReady.get()) {
            mainLoop.poll();
        }
    }
}
