package command;

public interface AppendEntriesCommand {
    AppendEntriesCommand term(long term);
    AppendEntriesCommand leaderId(long leaderId);
}
