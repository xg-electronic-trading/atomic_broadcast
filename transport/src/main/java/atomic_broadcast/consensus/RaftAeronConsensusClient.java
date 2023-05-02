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
import time.Clock;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static atomic_broadcast.aeron.AeronModule.CONSENSUS_STREAM_ID;
import static atomic_broadcast.utils.Action.CommandSent;

public class RaftAeronConsensusClient implements ConsensusTransportClient {

    private final Log log = LogFactory.getLog(this.getClass().getName());

    private final InstanceInfo instanceInfo;
    private final Clock clock;
    private final AeronClient aeronClient;
    private final List<ClusterMember> clusterMembers;
    private final List<CommandProcessor> cmdProcessors;
    private Subscription consensusSubscription;
    private final AeronConsensusFragmentHandler delegateHandler;
    private final FragmentHandler fragmentHandler;
    private final TransportParams consensusParams;
    private final BoundedRandomNumberGenerator randomNumberGenerator;
    private final long randomOffset;

    private final CommandBuilder cmdBuilder = new CommandBuilderImpl();

    public RaftAeronConsensusClient(InstanceInfo instanceInfo,
                                    Clock clock,
                                    AeronClient aeronClient,
                                    List<ClusterMember> clusterMembers,
                                    List<CommandProcessor> cmdProcessors,
                                    TransportParams consensusParams,
                                    int electionTimeoutSeconds
    ) {
        this.instanceInfo = instanceInfo;
        this.clock = clock;
        this.aeronClient = aeronClient;
        this.clusterMembers = clusterMembers;
        this.cmdProcessors = cmdProcessors;
        this.consensusParams = consensusParams;
        this.delegateHandler = new AeronConsensusFragmentHandler(consensusParams.listeners());
        this.fragmentHandler = new FragmentAssembler(delegateHandler);
        this.randomNumberGenerator = new BoundedRandomNumberGenerator(
                TimeUnit.SECONDS.toMillis(electionTimeoutSeconds)
        );
        this.randomOffset = randomNumberGenerator.generateRandom();

        log.info().append("app: ").append(instanceInfo.app())
                .append(", instance: ").append(instanceInfo.instance())
                .append(", election timeout millis: ").appendLast(randomOffset);
    }

    public void initialiseConsensusSubscription() {
        for (int i = 0; i < clusterMembers.size(); i++) {
            ClusterMember member = clusterMembers.get(i);
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
                if (isMessageOlderThanTimeout(consensusEventListener.lastMessageReceivedMillis(), expireTime)) {
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

    private boolean isMessageOlderThanTimeout(long lastMessageTime, long expireTime) {
        return expireTime > lastMessageTime;
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

        long votedFor = -1;
        long currentTerm = -1;
        boolean sent = false;
        for (int i = 0; i < clusterMembers.size(); i++) {
            ClusterMember member = clusterMembers.get(i);
            if (member.instance() == instanceInfo.instance()) {
                member.votedFor(instanceInfo.instance());
                member.incrementTerm();

                votedFor = member.votedFor();
                currentTerm = member.currentTerm();
                resetElectionTimeOut();
            }
        }

        for (int i = 0; i < cmdProcessors.size(); i++) {
            ClusterMember member = clusterMembers.get(i);
            CommandProcessor cmdProcessor = cmdProcessors.get(i);
            if (member.instance() != instanceInfo.instance()) {
                RequestVoteCommandImpl cmd = cmdBuilder
                        .createRequestVote();

                cmd.term(currentTerm)
                        .candidateId(votedFor)
                        .seqNo(0)
                        .logPosition(0);

                Action action = cmdProcessor.send(cmd);

                if (action == CommandSent) {
                    sent = true;
                }
            }
        }

        return sent;
    }

    @Override
    public void close() throws Exception {

    }
}
