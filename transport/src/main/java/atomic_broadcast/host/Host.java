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

import static atomic_broadcast.aeron.AeronModule.*;
import static atomic_broadcast.utils.App.AlgoContainer;
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
                cmdPublishers.add(new AeronPublisherClient(aeronClient, members.get(i).publicationChannel(), CONSENSUS_STREAM_ID));
            }
        }

        return cmdPublishers;
    }
    
    private List<CommandProcessor> createCommandProcessors(List<CommandPublisher> cmdPublishers, InstanceInfo instanceInfo) {
        List<CommandProcessor> cmdProcessors = new ArrayList<>(20);
        for (int i = 0; i < cmdPublishers.size(); i++) {
            CommandProcessor cmdProcessor = new CommandProcessorImpl(cmdPublishers.get(i), new NoOpCommandValidator(), instanceInfo);
            cmdProcessors.add(cmdProcessor);
        }

        return cmdProcessors;
    }

    private void createAndAddPublisherModules(List<CommandPublisher> cmdPublishers,
                                              TransportParams transportParams,
                                              InstanceInfo instanceInfo) {
        List<ClientPublisherModule> publisherModules = new ArrayList<>(20);
        for (int i = 0; i < cmdPublishers.size(); i++) {
            publisherModules.add(new ClientPublisherModule(cmdPublishers.get(i), transportParams, instanceInfo));
            pollables.add(publisherModules.get(i).transport());
            modules.add(publisherModules.get(i));
        }
    }

    public Host deploySequencer(TransportParams sequencerParams, TransportParams consensusParams) {
        InstanceInfo instanceInfo = new InstanceInfo(Sequencer, "localhost", sequencerParams.instanceId());
        AeronClient consensusAeronClient = new AeronClient(params, instanceInfo);

        ConsensusStateHolder consensusStateHolder = new ConsensusStateHolder(instanceInfo);
        List<CommandPublisher> cmdPublishers = createConsensusPublishers(instanceInfo, clusterMembers, consensusAeronClient);
        List<CommandProcessor> cmdProcessors = createCommandProcessors(cmdPublishers, instanceInfo);
        consensusParams.addListener(new ConsensusEventListener(clock, consensusStateHolder, clusterMembers.get(hostNum - 1), cmdProcessors));
        createAndAddPublisherModules(cmdPublishers, consensusParams, instanceInfo);

        ConsensusTransportClient consensusTransportClient = new RaftAeronConsensusClient(instanceInfo, clock, consensusAeronClient, clusterMembers, cmdProcessors, consensusParams, 5);
        consensus = new ConsensusModule(consensusTransportClient, consensusStateHolder, instanceInfo);
        pollables.add(consensus.transport());
        modules.add(consensusAeronClient);
        modules.add(consensus);

        AeronClient aeronClient = new AeronClient(params, instanceInfo);
        SeqNoClient seqNoClient = new SeqNoClient(new ShmSeqNoClient(sequencerParams.instanceId()));
        SequencerClient sequencerClient = new AeronSequencerClient(instanceInfo, aeronClient, sequencerParams, consensusStateHolder, seqNoClient);
        sequencer = new SequencerModule(sequencerParams, sequencerClient, consensusStateHolder, instanceInfo);
        pollables.add(sequencer.transport());
        modules.add(aeronClient);
        modules.add(sequencer);
        return this;
    }

    public Host deployClient(TransportParams transportParams, MessageListener listener) {
        InstanceInfo instanceInfo = new InstanceInfo(AlgoContainer, "localhost", transportParams.instanceId());
        AeronClient aeronClient = new AeronClient(params, instanceInfo);
        TransportClient transportClient = new AeronTransportClient(aeronClient, transportParams);
        CommandPublisher cmdPublisher = new AeronPublisherClient(aeronClient, transportParams.publicationChannel(), COMMAND_STREAM_ID);
        eventbus = new EventReaderModule(transportClient, transportParams, listener, instanceInfo);
        publisher = new ClientPublisherModule(cmdPublisher, transportParams, instanceInfo);
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
