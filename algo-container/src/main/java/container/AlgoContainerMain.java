package container;

import algo.AlgoCode;
import algo.AlgoCodeImpl;
import algo.AlgoContext;
import algo.AlgoContextImpl;
import atomic_broadcast.aeron.AeronClient;
import atomic_broadcast.aeron.AeronParams;
import atomic_broadcast.aeron.AeronPublisherClient;
import atomic_broadcast.aeron.AeronTransportClient;
import atomic_broadcast.client.*;
import atomic_broadcast.listener.MessageListener;
import atomic_broadcast.utils.*;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import command.CommandBuilder;
import command.CommandBuilderImpl;
import events.AlgoEventListener;
import events.AlgoOrderEventHandler;
import events.OrderEventHandler;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.CommonContext;
import orderstate.ByteBufferOrderStateCache;
import orderstate.OrderStateField;
import org.agrona.IoUtil;
import subscriptions.MarketDataHandler;
import subscriptions.MarketDataHandlerImpl;
import validator.AlgoCommandValidator;

import java.util.concurrent.atomic.AtomicBoolean;

import static atomic_broadcast.aeron.AeronModule.*;
import static atomic_broadcast.utils.App.AlgoContainer;

public class AlgoContainerMain {

    private static final Log log = LogFactory.getLog(AlgoContainerMain.class.getName());

    public static void main(String[] args) {
        try {
            start(new AlgoCodeImpl());
        } catch (Exception e) {
            log.error().append("error in AlgoContainerMain: ").appendLast(e);
        }
    }



    public static void start(AlgoCode algoCode) {
        InstanceInfo instanceInfo = new InstanceInfo(AlgoContainer, "localhost", 1);
        AtomicBoolean isReady = new AtomicBoolean();
        String aeronDir = IoUtil.tmpDirName()+"/"+"aeron";
        log.info().append("using aeron dir: ").appendLast(aeronDir);

        CompositeModule modules = new CompositeModule();
        /**
         * 1. setup client and aeron parameters
         */
        TransportParams clientParams = new TransportParams();
        clientParams
                .connectAs(ConnectAs.Client)
                .connectUsing(ConnectUsing.Unicast)
                .withEventReader(EventReaderType.RingBuffer)
                .addPublicationChannel(new ChannelUriStringBuilder()
                        .media(CommonContext.UDP_MEDIA)
                        .endpoint(COMMAND_ENDPOINT)
                        .build());

        AeronParams params = new AeronParams()
                .archivePort(ARCHIVE_REQUEST_PORT_RANGE_START)
                .aeronDir(aeronDir)
                .lowLatencyMode(false);

        /**
         * 2. set up aeron client
         * 3. set up transport client
         * 4. set up event reader
         */
        AeronClient aeronClient = new AeronClient(params, instanceInfo);
        TransportClient transportClient = new AeronTransportClient(aeronClient, clientParams, instanceInfo);

        /**
         * 5. set up aeron publisher client
         * 6. set up client publisher module
         * 7. set up cmd processor.
         */
        CommandPublisher cmdPublisher = new AeronPublisherClient(aeronClient, clientParams.publicationChannel(), COMMAND_STREAM_ID);
        ClientPublisherModule publisher = new ClientPublisherModule(cmdPublisher, clientParams, instanceInfo);
        CommandProcessor cmdProcessor = new CommandProcessorImpl(cmdPublisher, new NoOpCommandValidator(), instanceInfo);
        CommandBuilder cmdBuilder = new CommandBuilderImpl();

        OrderStateField[] fields = new OrderStateField[]{
                OrderStateField.Id,
                OrderStateField.Price,
                OrderStateField.Quantity,
                OrderStateField.MsgSeqNum};

        ByteBufferOrderStateCache cache = new ByteBufferOrderStateCache(false, 1, fields);
        AlgoContext ctx = new AlgoContextImpl(cmdProcessor, cmdBuilder);
        OrderEventHandler eventHandler = new AlgoOrderEventHandler(cache, ctx);
        MessageListener eventListener = new AlgoEventListener(eventHandler);

        EventReaderModule eventbus = new EventReaderModule(transportClient, clientParams, eventListener, instanceInfo);

        modules.add(aeronClient);
        modules.add(eventbus);
        modules.add(publisher);

        modules.start();

        Pollable mainLoop = new AlgoContainer(
                publisher.transport(),
                eventbus.eventsReader(), null);

        Runtime.getRuntime().addShutdownHook(new Thread(modules::close));

        while (isReady.get()) {
            mainLoop.poll();
        }
    }
}
