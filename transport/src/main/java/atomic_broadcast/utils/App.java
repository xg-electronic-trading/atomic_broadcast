package atomic_broadcast.utils;

public enum App implements CodedEnum {
    Sequencer(0),
    AlgoContainer(1),
    MarketGateway(2),
    FixGateway(3);

    private final int code;

    App(int code) {
        this.code = code;
    }

    @Override
    public int getCode() {
        return code;
    }
}
