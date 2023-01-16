package atomic_broadcast.utils;

public enum Action implements CodedEnum {
    NoAction(-1),
    CommandSent(0),
    CommandValidationFailed(1),
    CommandSurpressed(2);



    private final int code;

    Action(int code) {
        this.code = code;
    }

    @Override
    public int getCode() {
        return code;
    }
}
