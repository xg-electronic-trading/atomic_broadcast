package atomic_broadcast.aeron;

import reusable.Resettable;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import org.agrona.ErrorHandler;

import java.util.concurrent.atomic.AtomicBoolean;

public class AeronErrorHandler implements ErrorHandler, Resettable {

    private final Log log = LogFactory.getLog(this.getClass().getName());

    /**
     * set when an error internal to aeron or aeron archive is thrown by the aeron
     * conductor thread.
     */
    private final AtomicBoolean isInError = new AtomicBoolean();

    @Override
    public void onError(Throwable throwable) {
        log.error().appendLast(throwable);
        log.error().appendLast("setting isInError to true.");
        isInError.set(true);
    }

    public boolean isInError() {
        return isInError.get();
    }

    public void reset() {
        this.isInError.set(false);
    }
}
