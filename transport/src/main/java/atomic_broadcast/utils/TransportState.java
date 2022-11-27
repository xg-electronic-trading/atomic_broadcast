package atomic_broadcast.utils;

public enum TransportState implements CodedEnum {
    NoState(-1),
    ConnectToJournalSource(0),
    FindJournal(1),
    ConnectToEventStream(2),
    PollEventStream(3),

    //sequencer specific states
    AdvertiseSeqNum(4);


    private int code;

    TransportState(int code) {
        this.code = code;
    }
    @Override
    public int getCode() {
        return code;
    }
}
