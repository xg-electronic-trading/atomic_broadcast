package schema.api.consensus;

public interface AppendEntries {
    long term();
    long leaderId();
}
