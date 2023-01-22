package atomic_broadcast.utils;

public enum EventReaderType {
    Direct, //attach event listener directly to multicast event strea
    RingBuffer //attach event listener to ringbuffer
}
