package atomic_broadcast.consensus;

import atomic_broadcast.utils.CodedEnum;

public enum ClusterTransportState implements CodedEnum {

    Follower(0),
    Candidate(1),
    Leader(2);


    int code;

    ClusterTransportState(int code) {
        this.code = code;
    }

    @Override
    public int getCode() {
        return 0;
    }
}
