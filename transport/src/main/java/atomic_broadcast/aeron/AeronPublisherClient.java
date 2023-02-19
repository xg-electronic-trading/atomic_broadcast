package atomic_broadcast.aeron;

import atomic_broadcast.client.CommandPublisher;
import atomic_broadcast.utils.TransportParams;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.CommonContext;
import io.aeron.Publication;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;

import static atomic_broadcast.aeron.AeronModule.*;
import static io.aeron.Publication.*;

public class AeronPublisherClient implements CommandPublisher {

    private static final Log log = LogFactory.getLog(AeronPublisherClient.class.getName());

    private final AeronClient aeronClient;
    private Publication publication;

    private final BufferClaim bufferClaim = new BufferClaim();

    private final String commandStreamPublicationChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .endpoint(COMMAND_ENDPOINT)
            .build();

    public AeronPublisherClient(AeronClient aeronClient) {
        this.aeronClient = aeronClient;
    }

    @Override
    public boolean connectToCommandStream() {
        if (null == publication) {
            publication = aeronClient.addPublication(commandStreamPublicationChannel, COMMAND_STREAM_ID);
            return null != publication;
        } else {
            return true;
        }
    }

    @Override
    public boolean isPublicationConnected() {
        return null != publication && publication.isConnected();
    }

    @Override
    public boolean isPublicationClosed() {
        return null != publication && publication.isClosed();
    }

    @Override
    public boolean send(DirectBuffer buffer, int offset, int length) {
        if (null != publication) {
            if (length > publication.maxPayloadLength()) {
                /*
                 * send messages > MTU using standard offer().
                 * these will be fragmented over the wire.
                 */

                long result = publication.offer(buffer, offset, length);
                return processResult(result);
            } else {
                /*
                 * send messages <= MTU via tryClaim() using
                 * zero copy semantics for increased performance.
                 */

                long result = publication.tryClaim(length, bufferClaim);
                if (result > 0) {
                    bufferClaim.putBytes(buffer, offset, length);
                    bufferClaim.commit();
                } else {
                    bufferClaim.abort();
                }
                return processResult(result);
            }
        } else {
            return false;
        }
    }

    private boolean processResult(long publicationResult) {
        if (publicationResult == NOT_CONNECTED) {
            log.error().appendLast("publication not connected.");
            return false;
        } else if (publicationResult == BACK_PRESSURED) {
            log.error().appendLast("publication back pressured. please retry offer.");
            return false;
        } else if (publicationResult == ADMIN_ACTION) {
            log.error().appendLast("publication admin action. please retry offer.");
            return false;
        } else if (publicationResult == CLOSED) {
            log.error().appendLast("publication closed. cannot offer.");
            return false;
        } else if (publicationResult == MAX_POSITION_EXCEEDED) {
            log.error().appendLast("max position exceeded. publication should be closed and then a new one added.");
            return false;
        }

        return true;
    }

    @Override
    public void close() {
        aeronClient.closePublication(publication);
        publication = null;
    }
}
