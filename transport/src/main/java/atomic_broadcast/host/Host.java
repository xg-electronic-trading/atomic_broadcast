package atomic_broadcast.host;

import atomic_broadcast.aeron.*;
import atomic_broadcast.client.ClientPublisherModule;
import atomic_broadcast.client.CommandPublisher;
import atomic_broadcast.client.EventBusSubscriberModule;
import atomic_broadcast.client.TransportClient;
import atomic_broadcast.consensus.SeqNoClient;
import atomic_broadcast.consensus.SeqNoProvider;
import atomic_broadcast.consensus.ShmSeqNoClient;
import atomic_broadcast.consensus.ShmSeqNoServer;
import atomic_broadcast.sequencer.SequencerClient;
import atomic_broadcast.sequencer.SequencerModule;
import atomic_broadcast.utils.CompositeModule;
import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.TransportParams;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import org.agrona.IoUtil;
import time.Clock;
import time.RealClock;
import java.util.List;

public class Host {

    Log log = LogFactory.getLog(this.getClass().getName());

    private String alias;
    private CompositeModule modules;
    private AeronModule mediaDriver;
    private SequencerModule sequencer;
    private EventBusSubscriberModule eventbus;
    private ClientPublisherModule publisher;
    private final AeronParams params;
    private final Clock clock;

    public Host(String alias) {
        String aeronDir = IoUtil.tmpDirName()+"/"+alias+"/"+"aeron";
        log.info().append("using aeron dir: ").appendLast(aeronDir);

        this.alias = alias;
        this.clock = new RealClock();
        this.modules = new CompositeModule();
        this.params = new AeronParams()
                .commandPort(40001)
                .eventPort(40002)
                .archivePort(8010)
                .aeronDir(aeronDir)
                .lowLatencyMode(false)
                .clock(clock);
    }

    public Host deployShmSeqNoServer() {
        ShmSeqNoServer shmSeqNoServer = new ShmSeqNoServer();
        shmSeqNoServer.setReady(true);
        shmSeqNoServer.writeSeqNum(1, 1, 0);
        return this;
    }

    public Host deployMediaDriver() {
        mediaDriver = new AeronModule(params);
        modules.add(mediaDriver);
        return this;
    }

    public Host deploySequencer(TransportParams transportParams) {
        AeronClient aeronClient = new AeronClient(params);
        SequencerClient sequencerClient = new AeronSequencerClient(aeronClient, transportParams);
        SeqNoProvider seqNoProvider = new SeqNoClient(new ShmSeqNoClient());
        sequencer = new SequencerModule(transportParams, sequencerClient, seqNoProvider);
        modules.add(aeronClient);
        modules.add(sequencer);
        return this;
    }

    public Host deployClient(TransportParams transportParams) {
        AeronClient aeronClient = new AeronClient(params);
        TransportClient transportClient = new AeronTransportClient(aeronClient, transportParams);
        CommandPublisher cmdPublisher = new AeronPublisherClient(aeronClient);
        eventbus = new EventBusSubscriberModule(transportClient, transportParams);
        publisher = new ClientPublisherModule(cmdPublisher, transportParams);
        modules.add(aeronClient);
        modules.add(eventbus);
        modules.add(publisher);
        return this;
    }

    public void start() {
        modules.start();
    }

    public CompositeModule modules() {
        return modules;
    }

    public List<Module> moduleList() {
        return modules.getModules();
    }

    public void close() {
        modules.close();
    }

    public SequencerModule sequencer() {
        return sequencer;
    }

    public EventBusSubscriberModule eventbus() {
        return eventbus;
    }

    public ClientPublisherModule publisher() { return publisher; }

}
