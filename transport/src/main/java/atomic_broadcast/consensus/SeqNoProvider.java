package atomic_broadcast.consensus;

public interface SeqNoProvider {

    SeqNumSnapshot takeSnapshot();
}
