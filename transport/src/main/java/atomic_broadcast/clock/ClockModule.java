package atomic_broadcast.clock;

import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.ModuleName;
import time.Clock;

import static atomic_broadcast.utils.ModuleName.Clock;

public class ClockModule implements Module {

    private final Clock clock;

    public ClockModule(Clock clock) {
        this.clock = clock;
    }

    @Override
    public ModuleName name() {
        return Clock;
    }

    @Override
    public void start() {

    }

    @Override
    public void close() {

    }

    @Override
    public void poll() {
        clock.time();
    }
}
