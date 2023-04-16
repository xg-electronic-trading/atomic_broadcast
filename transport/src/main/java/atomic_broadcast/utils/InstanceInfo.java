package atomic_broadcast.utils;

public class InstanceInfo {

    private final App app;
    private final int instance;
    private final String hostname;

    public InstanceInfo(App app, String hostname, int instance) {
        this.app = app;
        this.hostname = hostname;
        this.instance = instance;
    }

    public App app() { return app; }

    public int instance() { return  instance; }

    public String hostname() { return hostname; }
}
