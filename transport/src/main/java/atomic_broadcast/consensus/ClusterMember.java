package atomic_broadcast.consensus;

import atomic_broadcast.utils.App;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.CommonContext;
import io.aeron.Publication;

import static atomic_broadcast.aeron.AeronModule.*;

public class ClusterMember {
    private final App app;
    private final int instance;
    private boolean votedGranted;

    private final ChannelUriStringBuilder publicationBuilder = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA);

    private final String publicationChannel;

    public ClusterMember(App app, int instance) {
        this.app = app;
        this.instance = instance;
        publicationChannel = publicationBuilder.endpoint(LOCAL_HOST + ":" + (CONSENSUS_PORT_RANGE_START + instance)).build();
    }

    public String publicationChannel() {
        return publicationChannel;
    }

    public long instance() {
        return instance;
    }

    public void setVotedGranted(boolean votedGranted) {
        this.votedGranted = votedGranted;
    }

    public boolean isVotedGranted() {
        return votedGranted;
    }
}
