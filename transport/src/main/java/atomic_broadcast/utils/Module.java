package atomic_broadcast.utils;

public interface Module {

    ModuleName name();

    void start();

    void close();
}
