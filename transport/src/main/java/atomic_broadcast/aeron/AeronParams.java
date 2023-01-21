package atomic_broadcast.aeron;

import atomic_broadcast.utils.Resettable;
import time.Clock;

public class AeronParams implements Resettable {
    private int commandPort = -1;
    private int eventPort = -1;
    private int archiveRequestPort = -1;
    private boolean lowLatencyMode = false;
    private String aeronDir = "";
    private Clock clock = null;

    public AeronParams() {
        reset();
    }

    @Override
    public void reset() {
        commandPort = -1;
        eventPort = -1;
        archiveRequestPort = -1;
        lowLatencyMode = false;
        aeronDir = "";
        clock = null;
    }

    public AeronParams commandPort(int port) {
        this.commandPort = port;
        return this;
    }

    public AeronParams eventPort(int port) {
        this.eventPort = port;
        return this;
    }

    public AeronParams archivePort(int port) {
        this.archiveRequestPort = port;
        return this;
    }

    public AeronParams lowLatencyMode(boolean lowLatency) {
        this.lowLatencyMode = lowLatency;
        return this;
    }

    public AeronParams aeronDir(String aeronDir) {
        this.aeronDir = aeronDir;
        return this;
    }

    public AeronParams clock(Clock clock) {
        this.clock = clock;
        return this;
    }

    public int eventPort() {
        return eventPort;
    }

    public int commandPort() {
        return commandPort;
    }

    public int archiveRequestPort() {
        return archiveRequestPort;
    }

    public boolean lowLatencyMode() {
        return lowLatencyMode;
    }

    public String aeronDir() {
        return aeronDir;
    }

    public Clock clock() { return clock; }
}
