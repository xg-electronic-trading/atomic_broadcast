package atomic_broadcast.client;

import atomic_broadcast.utils.Action;
import command.Command;

public interface CommandProcessor {

    Action send(Command cmd);
}
