package atomic_broadcast.utils;

public interface Module {

    void start();

    void close();

    void poll();
}
