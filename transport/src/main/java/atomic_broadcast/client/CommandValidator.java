package atomic_broadcast.client;

import atomic_broadcast.utils.ValidationResult;
import command.Command;

public interface CommandValidator {

    ValidationResult validate(Command cmd);
}
