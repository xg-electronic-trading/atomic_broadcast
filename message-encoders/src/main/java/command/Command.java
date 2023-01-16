package command;

import schema.api.Packet;

public interface Command {

    void beginWrite();
    void endWrite();
    int encodedLength();
    Packet packet();
}
