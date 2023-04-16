package atomic_broadcast.consensus;

public interface SeqNumSnapshot {

    long seqNo();

    int instance();
}
