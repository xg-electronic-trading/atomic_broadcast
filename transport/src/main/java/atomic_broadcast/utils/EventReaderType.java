package atomic_broadcast.utils;

public enum EventReaderType {
    Direct, //attach event listener directly to multicast event stream
    RingBuffer //attach event listener to ringbuffer
}
