package atomic_broadcast.client;

import atomic_broadcast.utils.Action;
import atomic_broadcast.utils.InstanceInfo;
import atomic_broadcast.utils.ValidationResult;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import command.Command;

import static atomic_broadcast.utils.Action.CommandSent;
import static atomic_broadcast.utils.Action.CommandSurpressed;

public class CommandProcessorImpl implements CommandProcessor {

    private final Log log = LogFactory.getLog(this.getClass().getName());

    private final CommandPublisher commandPublisher;
    private final CommandValidator commandValidator;
    private final InstanceInfo instanceInfo;

    public CommandProcessorImpl(CommandPublisher commandPublisher,
                                CommandValidator commandValidator,
                                InstanceInfo instanceInfo) {
        this.commandPublisher = commandPublisher;
        this.commandValidator = commandValidator;
        this.instanceInfo = instanceInfo;
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
                logCmd(cmd, CommandSent);
                return CommandSent;
            } else {
                logCmd(cmd, CommandSurpressed);
                return CommandSurpressed;
            }
        } else {
            return CommandSurpressed;
        }
    }

    private void logCmd(Command cmd, Action action) {
        if (log.isDebugEnabled()) {
            log.debug().append(cmd.toString())
                    .append(", action: ").appendLast(action);
        }
    }
}
