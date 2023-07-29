package atomic_broadcast.utils;

public enum JournalState implements CodedEnum {

    InactiveJournal(0), // journal that is not being recorded to.
    ActiveJournal(1), // journal that is actively being recorded to.
    JournalNotFound(2);

    private final int code;

    JournalState(int code) {
        this.code = code;
    }

    @Override
    public int getCode() {
        return code;
    }
}
