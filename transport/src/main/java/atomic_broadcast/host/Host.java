package atomic_broadcast.host;

import atomic_broadcast.aeron.AeronModule;
import atomic_broadcast.aeron.AeronSequencerClient;
import atomic_broadcast.aeron.AeronTransportClient;
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

public class Host {

    private String alias;
    private CompositeModule modules;
    private AeronModule mediaDriver;
    private SequencerModule sequencer;
    private EventBusTransportModule eventbus;

    public Host(String alias) {
        this.alias = alias;
        this.modules = new CompositeModule();
    }

    public Host deployShmSeqNoServer() {
        ShmSeqNoServer shmSeqNoServer = new ShmSeqNoServer();
        shmSeqNoServer.setReady(true);
        shmSeqNoServer.writeSeqNum(1, 1, 0);
        return this;
    }

    public Host deployMediaDriver() {
        mediaDriver = new AeronModule(true, false, false);
        modules.add(mediaDriver);
        return this;
    }

    public Host deploySequencer(TransportParams params) {
        AeronModule aeronModule = new AeronModule(false, true, false);
        SequencerClient sequencerClient = new AeronSequencerClient(aeronModule, params);
        SeqNoProvider seqNoProvider = new SeqNoClient(new ShmSeqNoClient());
        sequencer = new SequencerModule(params, sequencerClient, seqNoProvider);
        modules.add(aeronModule);
        modules.add(sequencer);
        return this;
    }

    public Host deployClient(TransportParams params) {
        AeronModule aeronModule = new AeronModule(false, true, false);
        TransportClient transportClient = new AeronTransportClient(aeronModule, params);
        eventbus = new EventBusTransportModule(transportClient, params);
        modules.add(aeronModule);
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
