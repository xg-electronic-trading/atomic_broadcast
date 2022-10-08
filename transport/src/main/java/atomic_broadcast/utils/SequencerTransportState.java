package atomic_broadcast.utils;

public enum SequencerTransportState {

    NO_STATE,
    CONNECT_TO_ARCHIVE,
    REPLAY,
    ADVERTISE_SEQ_NUM,
    CONNECT_TO_CMD_EVT_BUS,
    TAIL_REPLICATED_RECORDING
}
