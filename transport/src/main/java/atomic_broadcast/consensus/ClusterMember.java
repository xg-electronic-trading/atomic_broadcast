package atomic_broadcast.consensus;

import atomic_broadcast.utils.App;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.CommonContext;
import io.aeron.Publication;

import static atomic_broadcast.aeron.AeronModule.*;

public class ClusterMember {

    private long currentTerm; //last term (election cycle) server has seen
    private long votedFor; //instance id that received vote in current term.
    private final App app;
    private final int instance;

    private final ChannelUriStringBuilder publicationBuilder = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA);

    private final String publicationChannel;

    public ClusterMember(App app, int instance) {
        this.app = app;
        this.instance = instance;
        publicationChannel = publicationBuilder.endpoint(LOCAL_HOST + ":" + (CONSENSUS_PORT_RANGE_START + instance)).build();
    }

    public void incrementTerm() {
        currentTerm++;
    }

    public void votedFor(long instance) {
        votedFor = instance;
    }

    public long currentTerm() {
        return currentTerm;
    }

    public long votedFor() {
        return votedFor;
    }

    public String publicationChannel() {
        return publicationChannel;
    }

    public long instance() {
        return instance;
    }
}
