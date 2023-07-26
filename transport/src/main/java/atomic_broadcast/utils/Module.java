package atomic_broadcast.utils;

public interface Module {

    ModuleName name();

    InstanceInfo instanceInfo();

    boolean isStarted();

    void start();

    void close();
}
