package atomic_broadcast.utils;

public enum ValidationResult implements CodedEnum {
    ValidationPassed(0),
    ValidationFailed(1);

    private final int code;

    ValidationResult(int code) {
        this.code = code;
    }
    @Override
    public int getCode() {
        return code;
    }
}
