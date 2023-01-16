package atomic_broadcast.client;

import atomic_broadcast.utils.Action;
import atomic_broadcast.utils.ValidationResult;
import command.Command;

import static atomic_broadcast.utils.Action.CommandSent;
import static atomic_broadcast.utils.Action.CommandSurpressed;

public class CommandProcessorImpl implements CommandProcessor {

    private final CommandPublisher commandPublisher;
    private final CommandValidator commandValidator;

    public CommandProcessorImpl(CommandPublisher commandPublisher, CommandValidator commandValidator) {
        this.commandPublisher = commandPublisher;
        this.commandValidator = commandValidator;
    }

    @Override
    public Action send(Command cmd) {
        ValidationResult result = commandValidator.validate(cmd);
        try {
            switch (result) {
                case ValidationPassed:
                    return tryCommandSend(cmd);
                case ValidationFailed:
                    return CommandSurpressed;
                default:
                    throw new IllegalArgumentException("unknown validation result: " + result);
            }
        } catch (Exception e) {
            //log exception and close
            return CommandSurpressed;
        } finally {
            cmd.endWrite();
        }
    }

    private Action tryCommandSend(Command cmd) {
        if (commandPublisher.isPublicationConnected()) {
            if (commandPublisher.send(
                    cmd.packet().buffer(),
                    0,
                    cmd.packet().offset() + cmd.encodedLength())
            ) {
                return CommandSent;
            } else {
                return CommandSurpressed;
            }
        } else {
            return CommandSurpressed;
        }
    }
}
