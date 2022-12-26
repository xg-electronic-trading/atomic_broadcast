package atomic_broadcast.host;

import atomic_broadcast.aeron.*;
import atomic_broadcast.client.EventBusTransportModule;
import atomic_broadcast.client.TransportClient;
import atomic_broadcast.consensus.SeqNoClient;
import atomic_broadcast.consensus.SeqNoProvider;
import atomic_broadcast.consensus.ShmSeqNoClient;
import atomic_broadcast.consensus.ShmSeqNoServer;
import atomic_broadcast.sequencer.SequencerClient;
import atomic_broadcast.sequencer.SequencerModule;
import atomic_broadcast.utils.CompositeModule;
import atomic_broadcast.utils.TransportParams;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import org.agrona.IoUtil;

public class Host {

    Log log = LogFactory.getLog(this.getClass().getName());

    private String alias;
    private CompositeModule modules;
    private AeronModule mediaDriver;
    private SequencerModule sequencer;
    private EventBusTransportModule eventbus;
    private final AeronParams params;

    public Host(String alias) {
        String aeronDir = IoUtil.tmpDirName()+"/"+alias+"/"+"aeron";
        log.info().append("using aeron dir: ").appendLast(aeronDir);

        this.alias = alias;
        this.modules = new CompositeModule();
        this.params = new AeronParams()
                .commandPort(40001)
                .eventPort(40002)
                .archivePort(8010)
                .aeronDir(aeronDir)
                .lowLatencyMode(false);
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
        eventbus = new EventBusTransportModule(transportClient, transportParams);
        modules.add(aeronClient);
        modules.add(eventbus);
        return this;
    }

    public void start() {
        modules.start();
    }

    public CompositeModule modules() {
        return modules;
    }

    public void close() {
        modules.close();
    }

    public SequencerModule sequencer() {
        return sequencer;
    }

    public EventBusTransportModule eventbus() {
        return eventbus;
    }



}
