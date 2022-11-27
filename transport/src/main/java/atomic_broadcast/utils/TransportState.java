package atomic_broadcast.utils;

public enum TransportState implements CodedEnum {
    NoState(-1),
    ConnectToJournalSource(0),
    FindJournal(1),
    CreateNewJournal(2),
    ConnectToEventStream(3),
    PollEventStream(4),

    //sequencer specific states
    FindLeader(5),
    StartReplication(6),
    StopRepliaction(7);



    private int code;

    TransportState(int code) {
        this.code = code;
    }
    @Override
    public int getCode() {
        return code;
    }
}
