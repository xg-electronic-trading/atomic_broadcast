package atomic_broadcast.utils;

public enum ReplayState implements CodedEnum {

    NotStarted(0),
    Started(1),
    Skipped(2),
    Stopped(3);

    private final int code;

    ReplayState(int code) {
        this.code = code;
    }

    @Override
    public int getCode() {
        return code;
    }
}
