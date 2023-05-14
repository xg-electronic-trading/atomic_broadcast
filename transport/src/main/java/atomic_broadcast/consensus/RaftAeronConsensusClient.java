package atomic_broadcast.consensus;

import atomic_broadcast.aeron.AeronClient;
import atomic_broadcast.aeron.AeronConsensusFragmentHandler;
import atomic_broadcast.client.CommandProcessor;
import atomic_broadcast.listener.MessageListener;
import atomic_broadcast.utils.Action;
import atomic_broadcast.utils.BoundedRandomNumberGenerator;
import atomic_broadcast.utils.InstanceInfo;
import atomic_broadcast.utils.TransportParams;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import command.*;
import io.aeron.FragmentAssembler;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.collections.Long2ObjectHashMap;
import time.Clock;

import java.util.concurrent.TimeUnit;

import static atomic_broadcast.aeron.AeronModule.CONSENSUS_STREAM_ID;
import static atomic_broadcast.utils.Action.CommandSent;

public class RaftAeronConsensusClient implements ConsensusTransportClient {

    private final Log log = LogFactory.getLog(this.getClass().getName());

    private final InstanceInfo instanceInfo;
    private final Clock clock;
    private final AeronClient aeronClient;
    private final Long2ObjectHashMap<ClusterMember> clusterMembers;
    private final Long2ObjectHashMap<CommandProcessor> cmdProcessors;
    private Subscription consensusSubscription;
    private final AeronConsensusFragmentHandler delegateHandler;
    private final FragmentHandler fragmentHandler;
    private final TransportParams consensusParams;
    private final SeqNoClient seqNoClient;
    private final BoundedRandomNumberGenerator randomNumberGenerator;
    private final ConsensusStateHolder consensusStateHolder;
    private final long randomOffset;
    private final long hearbeatIntervalMillis;

    private long lastHeartbeatSendTime = 0;

    private final CommandBuilder cmdBuilder = new CommandBuilderImpl();

    public RaftAeronConsensusClient(InstanceInfo instanceInfo,
                                    Clock clock,
                                    AeronClient aeronClient,
                                    Long2ObjectHashMap<ClusterMember> clusterMembers,
                                    Long2ObjectHashMap<CommandProcessor> cmdProcessors,
                                    TransportParams consensusParams,
                                    SeqNoClient seqNoClient,
                                    ConsensusStateHolder consensusStateHolder,
                                    int electionTimeoutSeconds,
                                    int heartbeatIntervalSeconds
    ) {
        this.instanceInfo = instanceInfo;
        this.clock = clock;
        this.aeronClient = aeronClient;
        this.clusterMembers = clusterMembers;
        this.cmdProcessors = cmdProcessors;
        this.consensusParams = consensusParams;
        this.seqNoClient = seqNoClient;
        this.delegateHandler = new AeronConsensusFragmentHandler(consensusParams.listeners());
        this.fragmentHandler = new FragmentAssembler(delegateHandler);
        this.randomNumberGenerator = new BoundedRandomNumberGenerator(
                TimeUnit.SECONDS.toMillis(electionTimeoutSeconds)
        );
        this.consensusStateHolder = consensusStateHolder;
        this.randomOffset = randomNumberGenerator.generateRandom();
        this.hearbeatIntervalMillis = TimeUnit.SECONDS.toMillis(heartbeatIntervalSeconds);

        log.info().append("app: ").append(instanceInfo.app())
                .append(", instance: ").append(instanceInfo.instance())
                .append(", heartbeat interval seconds: ")
                .append(", election timeout millis: ").appendLast(randomOffset);
    }

    public void initialiseConsensusSubscription() {
        for (ClusterMember member : clusterMembers.values()) {
            if (member.instance() == instanceInfo.instance()) {
                consensusSubscription = aeronClient.addSubscription(member.publicationChannel(), CONSENSUS_STREAM_ID);
            }
        }

    }

    @Override
    public void initialise() {
        initialiseConsensusSubscription();
    }

    @Override
    public boolean pollSubscription() {
        consensusSubscription.poll(fragmentHandler, 100);
        return true;
    }

    @Override
    public boolean hasHeartbeatTimeoutExpired() {
        for (int i = 0; i < consensusParams.listeners().size(); i++) {
            MessageListener listener = consensusParams.listeners().get(i);
            if (listener instanceof ConsensusEventListener) {
                ConsensusEventListener consensusEventListener = (ConsensusEventListener) listener;
                long expireTime = clock.time() - randomOffset;
                if (lookbackPeriodExpired(consensusEventListener.lastMessageReceivedMillis(), expireTime)) {
                    log.info().append("app: ").append(instanceInfo.app())
                            .append(", instance: ").append(instanceInfo.instance())
                            .append(", lastMessageReceivedMillis: ").append(consensusEventListener.lastMessageReceivedMillis())
                            .append(", expireTime: ").append(expireTime)
                            .append(", timeout expired: ").appendLast(true);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean lookbackPeriodExpired(long lastUpdateTime, long lookbackTime) {
        return lookbackTime > lastUpdateTime;
    }

    private void resetElectionTimeOut() {
        for (int i = 0; i < consensusParams.listeners().size(); i++) {
            MessageListener listener = consensusParams.listeners().get(i);
            if (listener instanceof ConsensusEventListener) {
                ConsensusEventListener consensusEventListener = (ConsensusEventListener) listener;
                consensusEventListener.updateLastMessageReceivedMillis();
            }
        }
    }

    /**
     * when state = Candidate:
     *  - increment currentTerm
     *  - vote for self
     *  - send RequestVote to all servers in cluster
     */

    @Override
    public boolean startElection() {
        ConsensusStateSnapshot consensusStateSnapshot = seqNoClient.readSeqNum();
        clusterMembers.get(instanceInfo.instance()).setVotedGranted(true);
        resetElectionTimeOut();

        boolean sent = false;

        for (CommandProcessor cmdProcessor : cmdProcessors.values()) {
            RequestVoteCommandImpl cmd = cmdBuilder
                    .createRequestVote();

            cmd.term(consensusStateSnapshot.currentTerm() + 1)
                    .candidateId(instanceInfo.instance())
                    .seqNo(consensusStateSnapshot.seqNo())
                    .logPosition(consensusStateSnapshot.logPosition());

            Action action = cmdProcessor.send(cmd);

            if (action == CommandSent) {
                consensusStateHolder.incrementActiveClusterMembers();
                sent = true;
            }
        }

        if (sent) {
            seqNoClient.writeConsensusState(consensusStateSnapshot.currentTerm() + 1, instanceInfo.instance());
        }

        return sent;
    }

    @Override
    public boolean sendHeartbeat() {
        boolean sent = false;
        ConsensusStateSnapshot consensusStateSnapshot = seqNoClient.readSeqNum();

        long lookbackPeriod = clock.time() - hearbeatIntervalMillis;
        if (0 == lastHeartbeatSendTime || lookbackPeriodExpired(lastHeartbeatSendTime, lookbackPeriod)) {
            for (CommandProcessor cmdProcessor : cmdProcessors.values()) {
                AppendEntriesCommandimpl cmd = cmdBuilder.createAppendEntries();
                cmd.leaderId(instanceInfo.instance())
                        .term(consensusStateSnapshot.currentTerm());

                Action action = cmdProcessor.send(cmd);

                if (action == CommandSent) {
                    sent = true;
                    lastHeartbeatSendTime = clock.time();
                }
            }
        }

        return sent;
    }

    @Override
    public void close() throws Exception {

    }
}
