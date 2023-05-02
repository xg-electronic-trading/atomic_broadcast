package atomic_broadcast.consensus;

import atomic_broadcast.client.CommandProcessor;
import atomic_broadcast.listener.MessageListener;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import schema.api.Packet;
import schema.api.consensus.*;
import time.Clock;

import java.util.List;

import static atomic_broadcast.consensus.ClusterTransportState.Follower;
import static schema.api.MessageType.*;

public class ConsensusEventListener implements MessageListener {

    private static final Log log = LogFactory.getLog(ConsensusEventListener.class.getName());

    private final AppendEntriesImpl appendEntries = new AppendEntriesImpl();
    private final RequestVoteImpl requestVote = new RequestVoteImpl();
    private final RequestVoteResponseImpl requestVoteResponse = new RequestVoteResponseImpl();
    private final Clock clock;
    private final ConsensusStateHolder consensusStateHolder;
    private final List<CommandProcessor> commandProcessors;
    private final ClusterMember clusterMember;
    private long lastMessageReceivedMillis;

    public ConsensusEventListener(Clock clock,
                                  ConsensusStateHolder consensusStateHolder,
                                  ClusterMember clusterMember,
                                  List<CommandProcessor> commandProcessors) {
        this.clock = clock;
        this.lastMessageReceivedMillis = clock.time();
        this.consensusStateHolder = consensusStateHolder;
        this.clusterMember = clusterMember;
        this.commandProcessors = commandProcessors;
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
        if (appendEntries.term() < clusterMember.currentTerm()) {
            log.warn().append("received heartbeat from ").append(appendEntries.leaderId())
                    .append(" which has term: ").append(appendEntries.term())
                    .append(" less than currentTerm: ").appendLast(clusterMember.currentTerm());
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
        if (requestVote.term() >= clusterMember.currentTerm()) {
            /** check seqnum and log position of candiate are atleast as up
             * to date as receiver before granting vote
             */

        } else {
            /**
             * candidate term is lower than receivers
             * term. reply with voteGranted = false
             */
        }
    }

    private void handleRequestVoteResponse(RequestVoteResponse requestVoteResponse) {

    }

    private void printEvent(Object object) {
        if (log.isDebugEnabled()) {
            log.debug().appendLast(object.toString());
        }
    }
}
