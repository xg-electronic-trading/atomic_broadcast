package atomic_broadcast.consensus;

import atomic_broadcast.client.CommandProcessor;
import atomic_broadcast.listener.MessageListener;
import atomic_broadcast.utils.ConsensusUtils;
import atomic_broadcast.utils.InstanceInfo;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import command.*;
import org.agrona.collections.Long2ObjectHashMap;
import schema.api.Packet;
import schema.api.consensus.*;
import time.Clock;

import java.util.List;

import static atomic_broadcast.consensus.ClusterTransportState.Follower;
import static atomic_broadcast.consensus.ClusterTransportState.Leader;
import static schema.api.MessageType.*;

public class ConsensusEventListener implements MessageListener {

    private static final Log log = LogFactory.getLog(ConsensusEventListener.class.getName());

    private final InstanceInfo instanceInfo;
    private final CommandBuilder cmdBuilder = new CommandBuilderImpl();

    private final AppendEntriesImpl appendEntries = new AppendEntriesImpl();
    private final RequestVoteImpl requestVote = new RequestVoteImpl();
    private final RequestVoteResponseImpl requestVoteResponse = new RequestVoteResponseImpl();
    private final Clock clock;
    private final ConsensusStateHolder consensusStateHolder;
    private final Long2ObjectHashMap<CommandProcessor> commandProcessors;
    private final Long2ObjectHashMap<ClusterMember> clusterMembers;
    private final SeqNoClient seqNoClient;
    private long lastMessageReceivedMillis;

    public ConsensusEventListener(InstanceInfo instanceInfo,
                                  Clock clock,
                                  ConsensusStateHolder consensusStateHolder,
                                  Long2ObjectHashMap<ClusterMember> clusterMembers,
                                  Long2ObjectHashMap<CommandProcessor> commandProcessors,
                                  SeqNoClient seqNoClient) {
        this.instanceInfo = instanceInfo;
        this.clock = clock;
        this.lastMessageReceivedMillis = clock.time();
        this.consensusStateHolder = consensusStateHolder;
        this.clusterMembers = clusterMembers;
        this.commandProcessors = commandProcessors;
        this.seqNoClient = seqNoClient;
    }

    public long lastMessageReceivedMillis() {
        return lastMessageReceivedMillis;
    }

    public void updateLastMessageReceivedMillis() {
        lastMessageReceivedMillis = clock.time();
    }

    @Override
    public void onMessage(Packet packet) {
        switch (packet.messageType()) {
            case AppendEntries:
                appendEntries.init(packet);
                printEvent(appendEntries);
                handleAppendEntries(appendEntries);
                break;
            case RequestVote:
                requestVote.init(packet);
                printEvent(requestVote);
                handleRequestVote(requestVote);
                break;
            case RequestVoteResponse:
                requestVoteResponse.init(packet);
                printEvent(requestVoteResponse);
                handleRequestVoteResponse(requestVoteResponse);
                break;
            default:
                throw new IllegalArgumentException("unknown packet message type: " + packet.messageType());
        }
    }


    private void handleAppendEntries(AppendEntries appendEntries) {
        ConsensusStateSnapshot consensusStateSnapshot = seqNoClient.readSeqNum();

        if (appendEntries.term() < consensusStateSnapshot.currentTerm()) {
            log.warn().append("received heartbeat from ").append(appendEntries.leaderId())
                    .append(" which has term: ").append(appendEntries.term())
                    .append(" less than currentTerm: ").appendLast(consensusStateSnapshot.currentTerm());
        } else {
            lastMessageReceivedMillis = clock.time();

            if (consensusStateHolder.isCandidate()) {
                consensusStateHolder.setState(Follower);
            }

            //consensusStateHolder.setLeaderHostname();
            consensusStateHolder.setLeaderInstance((int) appendEntries.leaderId());
        }
    }

    private void handleRequestVote(RequestVote requestVote) {
        /** check seqnum and log position of candiate are atleast as up
         * to date as receiver before granting vote
         */

        ConsensusStateSnapshot consensusStateSnapshot = seqNoClient.readSeqNum();

        boolean candidateTermCondition = requestVote.term() > consensusStateSnapshot.currentTerm();
        boolean candidateSeqNoCondition = requestVote.seqNo() >= consensusStateSnapshot.seqNo();
        boolean candidateLogPositionCondition = requestVote.logPosition() >= consensusStateSnapshot.logPosition();
        boolean grantVote = candidateTermCondition && candidateSeqNoCondition && candidateLogPositionCondition;

        log.info().append("instance=").append(instanceInfo.instance())
                .append(", consensusStateSnaphot: ")
                .append("currentTerm=").append(consensusStateSnapshot.currentTerm())
                .append(", seqNo=").append(consensusStateSnapshot.seqNo())
                .append(", logPosition=").appendLast(consensusStateSnapshot.logPosition());

        RequestVoteResponseCommandImpl cmd = cmdBuilder.createRequestVoteResponse();
        cmd.term(requestVote.term());
        cmd.instanceid(instanceInfo.instance());
        cmd.voteGranted(grantVote);

        CommandProcessor cmdProcessor = getCmdProcessorForResponse(requestVote.candidateId());

        if (grantVote) {
            consensusStateHolder.setState(Follower);
            seqNoClient.writeConsensusState(requestVote.term(), requestVote.candidateId());
            updateLastMessageReceivedMillis();
            log.info().append("granting vote to candidate: ").appendLast(requestVote.candidateId());
        } else if (candidateTermCondition) {
            consensusStateHolder.setState(Follower);
            seqNoClient.writeConsensusState(requestVote.term(), -1);
        }

        cmdProcessor.send(cmd);
    }

    private CommandProcessor getCmdProcessorForResponse(long instance) {
        CommandProcessor cmdProcessor = commandProcessors.get(instance);
        if (null == cmdProcessor) {
            String error = "cmdProcessor not found for instance: " + instance;
            log.error().appendLast(error);
            throw new IllegalArgumentException(error);
        }

        return cmdProcessor;
    }

    private void handleRequestVoteResponse(RequestVoteResponse requestVoteResponse) {
        ConsensusStateSnapshot consensusStateSnapshot = seqNoClient.readSeqNum();

        /**
         * server must always become a follower if term received is greater
         * than its own term.
         */

        if (requestVoteResponse.term() > consensusStateSnapshot.currentTerm()) {
            consensusStateHolder.setState(Follower);
        } else if (requestVoteResponse.voteGranted()) {
            ClusterMember member = clusterMembers.get(requestVoteResponse.instanceId());
            if (null == member) {
                log.error().append("received vote from unknown cluster instance: ").append(requestVoteResponse.instanceId())
                        .appendLast(", not counting vote towards threshold for quorum. ");
            } else {
                member.setVotedGranted(true);
            }

            int votes = 0;
            for (ClusterMember clusterMember : clusterMembers.values()) {
                if (clusterMember.isVotedGranted()) {
                    votes++;
                }
            }

            boolean isLeader = votes >= ConsensusUtils.quorumThreshold(consensusStateHolder.getNoOfActiveClusterMembers());

            if (isLeader) {
                consensusStateHolder.setState(Leader);
                consensusStateHolder.setLeaderInstance(instanceInfo.instance());

                log.info().append("app: ").append(instanceInfo.app())
                        .append(", instance: ").append(instanceInfo.instance())
                        .append(", received ").append(votes).append(" votes. ")
                        .appendLast(" Assuming leadership.");
            }
        }
    }

    private void printEvent(Object object) {
        if (log.isDebugEnabled()) {
            log.debug().appendLast(object.toString());
        }
    }
}
