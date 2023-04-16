package atomic_broadcast.host;

import atomic_broadcast.aeron.*;
import atomic_broadcast.client.*;
import atomic_broadcast.consensus.*;
import atomic_broadcast.listener.MessageListener;
import atomic_broadcast.sequencer.SequencerClient;
import atomic_broadcast.sequencer.SequencerModule;
import atomic_broadcast.utils.*;
import atomic_broadcast.utils.Module;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import org.agrona.IoUtil;
import time.Clock;
import time.RealClock;

import java.util.ArrayList;
import java.util.List;

import static atomic_broadcast.aeron.AeronModule.ARCHIVE_REQUEST_PORT_RANGE_START;
import static atomic_broadcast.utils.App.Sequencer;

public class Host {

    Log log = LogFactory.getLog(this.getClass().getName());

    private int hostNum;
    private CompositeModule modules;
    private List<Pollable> pollables;
    private final List<ClusterMember> clusterMembers;
    private AeronModule mediaDriver;
    private SequencerModule sequencer;
    private ConsensusModule consensus;
    private EventReaderModule eventbus;
    private ClientPublisherModule publisher;
    private final AeronParams params;
    private final Clock clock;

    public Host(int hostNum) {
        String aeronDir = IoUtil.tmpDirName()+"/"+hostNum+"/"+"aeron";
        log.info().append("using aeron dir: ").appendLast(aeronDir);

        this.hostNum = hostNum;
        this.pollables = new ArrayList<>(20);
        this.clusterMembers = new ArrayList<>(20);
        this.clusterMembers.add(new ClusterMember(Sequencer, 1));
        this.clusterMembers.add(new ClusterMember(Sequencer, 2));
        this.clusterMembers.add(new ClusterMember(Sequencer, 3));
        this.clock = new RealClock();
        this.modules = new CompositeModule();
        this.params = new AeronParams()
                .archivePort(ARCHIVE_REQUEST_PORT_RANGE_START + hostNum)
                .aeronDir(aeronDir)
                .lowLatencyMode(false)
                .clock(clock);
    }

    public Host deployMediaDriver() {
        mediaDriver = new AeronModule(params);
        modules.add(mediaDriver);
        return this;
    }

    private List<CommandPublisher> createConsensusPublishers(InstanceInfo instanceInfo, List<ClusterMember> members, AeronClient aeronClient) {
        List<CommandPublisher> cmdPublishers = new ArrayList<>(20);
        for (int i = 0; i < members.size(); i++) {
            if (instanceInfo.instance() != members.get(i).instance()) {
                cmdPublishers.add(new AeronPublisherClient(aeronClient, members.get(i).publicationChannel()));
            }
        }

        return cmdPublishers;
    }
    
    private List<CommandProcessor> createCommandProcessors(List<CommandPublisher> cmdPublishers) {
        List<CommandProcessor> cmdProcessors = new ArrayList<>(20);
        for (int i = 0; i < cmdPublishers.size(); i++) {
            CommandProcessor cmdProcessor = new CommandProcessorImpl(cmdPublishers.get(i), new NoOpCommandValidator());
            cmdProcessors.add(cmdProcessor);
        }

        return cmdProcessors;
    }

    private void createAndAddPublisherModules(List<CommandPublisher> cmdPublishers, TransportParams transportParams) {
        List<ClientPublisherModule> publisherModules = new ArrayList<>(20);
        for (int i = 0; i < cmdPublishers.size(); i++) {
            publisherModules.add(new ClientPublisherModule(cmdPublishers.get(i), transportParams));
            pollables.add(publisherModules.get(i).transport());
            modules.add(publisherModules.get(i));
        }
    }

    public Host deploySequencer(TransportParams sequencerParams, TransportParams consensusParams) {
        InstanceInfo instanceInfo = new InstanceInfo(Sequencer, "localhost", sequencerParams.instanceId());
        AeronClient consensusAeronClient = new AeronClient(params);

        ConsensusStateHolder consensusStateHolder = new ConsensusStateHolder();
        List<CommandPublisher> cmdPublishers = createConsensusPublishers(instanceInfo, clusterMembers, consensusAeronClient);
        List<CommandProcessor> cmdProcessors = createCommandProcessors(cmdPublishers);
        consensusParams.addListener(new ConsensusEventListener(clock, consensusStateHolder, clusterMembers.get(hostNum - 1), cmdProcessors));
        createAndAddPublisherModules(cmdPublishers, consensusParams);

        ConsensusTransportClient consensusTransportClient = new RaftAeronConsensusClient(instanceInfo, clock, consensusAeronClient, clusterMembers, cmdProcessors, consensusParams);
        consensus = new ConsensusModule(consensusTransportClient, consensusStateHolder);
        pollables.add(consensus.transport());
        modules.add(consensusAeronClient);
        modules.add(consensus);

        AeronClient aeronClient = new AeronClient(params);
        SeqNoClient seqNoClient = new SeqNoClient(new ShmSeqNoClient(sequencerParams.instanceId()));
        SequencerClient sequencerClient = new AeronSequencerClient(instanceInfo, aeronClient, sequencerParams, consensusStateHolder, seqNoClient);
        sequencer = new SequencerModule(sequencerParams, sequencerClient, consensusStateHolder);
        pollables.add(sequencer.transport());
        modules.add(aeronClient);
        modules.add(sequencer);
        return this;
    }

    public Host deployClient(TransportParams transportParams, MessageListener listener) {
        AeronClient aeronClient = new AeronClient(params);
        TransportClient transportClient = new AeronTransportClient(aeronClient, transportParams);
        CommandPublisher cmdPublisher = new AeronPublisherClient(aeronClient, transportParams.publicationChannel());
        eventbus = new EventReaderModule(transportClient, transportParams, listener);
        publisher = new ClientPublisherModule(cmdPublisher, transportParams);
        pollables.add(eventbus.transport());
        pollables.add(eventbus.eventsReader());
        pollables.add(publisher.transport());
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

    public List<Pollable> pollables() {
        return pollables;
    }

    public void close() {
        modules.close();
    }

    public SequencerModule sequencer() {
        return sequencer;
    }

    public ConsensusModule consensus() {
        return consensus;
    }

    public EventReaderModule eventbus() {
        return eventbus;
    }

    public ClientPublisherModule publisher() { return publisher; }

    public int hostNum() { return hostNum; }

}
