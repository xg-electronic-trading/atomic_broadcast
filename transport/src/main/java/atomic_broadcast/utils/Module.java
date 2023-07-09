package atomic_broadcast.utils;

public interface Module {

    ModuleName name();

    InstanceInfo instanceInfo();

    void start();

    void close();
}
