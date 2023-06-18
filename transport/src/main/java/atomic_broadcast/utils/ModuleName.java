package atomic_broadcast.utils;

public enum ModuleName implements CodedEnum {
    Sequencer(0),
    ClientTransport(1),
    AeronMediaDriver(2),
    AeronClient(3),
    Composite(4),
    ClientPublisher(5),
    Clock(6),
    Consensus(7),
    Algo(8);


    private final int code;


    ModuleName(int code) {
        this.code = code;
    }

    @Override
    public int getCode() {
        return code;
    }
}
