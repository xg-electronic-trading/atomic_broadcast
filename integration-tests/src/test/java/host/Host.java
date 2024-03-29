package host;

import atomic_broadcast.aeron.*;
import atomic_broadcast.client.*;
import atomic_broadcast.consensus.*;
import atomic_broadcast.sample.SampleClient;
import atomic_broadcast.sequencer.SequencerClient;
import atomic_broadcast.sequencer.SequencerModule;
import atomic_broadcast.transport.TransportFactory;
import atomic_broadcast.utils.*;
import atomic_broadcast.utils.Module;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import listener.EventPrinter;
import org.agrona.IoUtil;
import org.agrona.collections.Long2ObjectHashMap;
import time.Clock;
import time.RealClock;
import java.util.List;
import java.util.Map;

import static atomic_broadcast.aeron.AeronModule.*;
import static atomic_broadcast.utils.App.*;

public class Host {

    Log log = LogFactory.getLog(this.getClass().getName());

    private int hostNum;
    private CompositeModule modules;
    private CompositePollable pollables;
    private final Long2ObjectHashMap<ClusterMember> clusterMembers;
    private AeronModule mediaDriver;
    private SequencerModule sequencer;
    private ConsensusModule consensus;
    private atomic_broadcast.sample.SampleClient client;
    private final AeronParams params;
    private final Clock clock;

    public Host(int hostNum) {
        String aeronDir = IoUtil.tmpDirName()+"/"+hostNum+"/"+"aeron";
        log.info().append("using aeron dir: ").appendLast(aeronDir);

        this.hostNum = hostNum;
        this.pollables = new CompositePollable();
        this.clusterMembers = new Long2ObjectHashMap<>(20, 0.65f, true);
        this.clusterMembers.put(1, new ClusterMember(Sequencer, "localhost", ARCHIVE_REQUEST_PORT_RANGE_START + 1, 1));
        this.clusterMembers.put(2, new ClusterMember(Sequencer, "localhost", ARCHIVE_REQUEST_PORT_RANGE_START + 2, 2));
        this.clusterMembers.put(3, new ClusterMember(Sequencer, "localhost", ARCHIVE_REQUEST_PORT_RANGE_START + 3, 3));
        this.clock = new RealClock();
        this.modules = new CompositeModule();
        this.params = new AeronParams()
                .archivePort(ARCHIVE_REQUEST_PORT_RANGE_START + hostNum)
                .aeronDir(aeronDir)
                .lowLatencyMode(false)
                .clock(clock);
    }

    public Host deployMediaDriver(int instance) {
        InstanceInfo instanceInfo = new InstanceInfo(MediaDriver, "localhost", instance);
        mediaDriver = new AeronModule(params, instanceInfo);
        modules.add(mediaDriver);
        return this;
    }

    private Long2ObjectHashMap<CommandPublisher> createConsensusPublishers(InstanceInfo instanceInfo, Long2ObjectHashMap<ClusterMember> members, AeronClient aeronClient) {
        Long2ObjectHashMap<CommandPublisher> cmdPublishers = new Long2ObjectHashMap<>(20, 0.65f, true);

        for (ClusterMember member : members.values()) {
            if (instanceInfo.instance() != member.instance()) {
                cmdPublishers.put(member.instance(), new AeronPublisherClient(aeronClient, member.publicationChannel(), CONSENSUS_STREAM_ID));
            }
        }

        return cmdPublishers;
    }
    
    private Long2ObjectHashMap<CommandProcessor> createCommandProcessors(Long2ObjectHashMap<CommandPublisher> cmdPublishers, InstanceInfo instanceInfo) {
        Long2ObjectHashMap<CommandProcessor> cmdProcessors = new Long2ObjectHashMap<>(20, 0.65f, true);
        Long2ObjectHashMap<CommandPublisher>.EntryIterator itr = cmdPublishers.entrySet().iterator();

        while (itr.hasNext()) {
            Map.Entry<Long, CommandPublisher> entry = itr.next();
            CommandProcessor cmdProcessor = new CommandProcessorImpl(entry.getValue(), new NoOpCommandValidator(), instanceInfo);
            cmdProcessors.put(entry.getKey(), cmdProcessor);
        }

        return cmdProcessors;
    }

    private void createAndAddPublisherModules(Long2ObjectHashMap<CommandPublisher> cmdPublishers,
                                              TransportParams transportParams,
                                              InstanceInfo instanceInfo) {
        Long2ObjectHashMap<CommandPublisher>.ValueIterator itr = cmdPublishers.values().iterator();
        while (itr.hasNext()) {
            ClientPublisherModule publisherModule = new ClientPublisherModule(itr.next(), transportParams, instanceInfo);
            pollables.add(publisherModule.transport());
            modules.add(publisherModule);
        }
    }

    public Host deploySequencer(TransportParams sequencerParams, TransportParams consensusParams) {
        InstanceInfo instanceInfo = new InstanceInfo(Sequencer, "localhost", sequencerParams.instanceId());
        SeqNoClient seqNoClient = new SeqNoClient(new ShmSeqNoClient(sequencerParams.instanceId()));
        AeronClient consensusAeronClient = new AeronClient(params, instanceInfo);

        ConsensusStateHolder consensusStateHolder = new ConsensusStateHolder(instanceInfo);
        Long2ObjectHashMap<CommandPublisher> cmdPublishers = createConsensusPublishers(instanceInfo, clusterMembers, consensusAeronClient);
        Long2ObjectHashMap<CommandProcessor> cmdProcessors = createCommandProcessors(cmdPublishers, instanceInfo);

        consensusParams.addListener(new ConsensusEventListener(
                instanceInfo,
                clock,
                consensusStateHolder,
                clusterMembers,
                cmdProcessors,
                seqNoClient));
        createAndAddPublisherModules(cmdPublishers, consensusParams, instanceInfo);

        ConsensusTransportClient consensusTransportClient = new RaftAeronConsensusClient(
                instanceInfo,
                clock,
                consensusAeronClient,
                clusterMembers,
                cmdProcessors,
                consensusParams,
                seqNoClient,
                consensusStateHolder,
                consensusParams.electionTimeoutSecs(),
                1);

        consensus = new ConsensusModule(consensusTransportClient, consensusStateHolder, instanceInfo);
        pollables.add(consensus.transport());
        modules.add(consensusAeronClient);
        modules.add(consensus);

        AeronClient aeronClient = new AeronClient(params, instanceInfo);
        SequencerClient sequencerClient = new AeronSequencerClient(
                instanceInfo,
                aeronClient,
                sequencerParams,
                consensusStateHolder,
                seqNoClient,
                clusterMembers,
                clock);

        sequencer = new SequencerModule(sequencerParams, sequencerClient, consensusStateHolder, instanceInfo);
        pollables.add(sequencer.transport());
        modules.add(aeronClient);
        modules.add(sequencer);
        return this;
    }

    public Host deployClient(Module client) {
        modules.add(client);
        return this;
    }

    public Host deploySampleClient(TransportParams transportParams) {
        InstanceInfo instanceInfo = new InstanceInfo(SampleClient, "localhost", transportParams.instanceId());
        TransportFactory transportFactory = new AeronTransportFactory(
                instanceInfo,
                transportParams,
                params,
                modules);

        client = new SampleClient(
                instanceInfo,
                transportParams,
                transportFactory,
                new EventPrinter(),
                modules,
                pollables);

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
        return pollables.getPollables();
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

    public atomic_broadcast.sample.SampleClient sampleClient() {
        return client;
    }

    public int hostNum() { return hostNum; }

}
