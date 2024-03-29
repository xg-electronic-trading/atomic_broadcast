package atomic_broadcast.clock;

import atomic_broadcast.utils.InstanceInfo;
import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.ModuleName;
import time.Clock;

import static atomic_broadcast.utils.ModuleName.Clock;

public class ClockModule implements Module {

    private final Clock clock;
    private final InstanceInfo instanceInfo;

    private boolean started;

    public ClockModule(Clock clock, InstanceInfo instanceInfo) {
        this.clock = clock;
        this.instanceInfo = instanceInfo;
    }

    @Override
    public ModuleName name() {
        return Clock;
    }

    @Override
    public InstanceInfo instanceInfo() {
        return instanceInfo;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void start() {
        started = true;
    }

    @Override
    public void close() {
        started = false;
    }
}
