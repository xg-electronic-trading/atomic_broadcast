package atomic_broadcast.client;

import atomic_broadcast.utils.ValidationResult;
import command.Command;

import static atomic_broadcast.utils.ValidationResult.ValidationPassed;

public class NoOpCommandValidator implements CommandValidator {

    @Override
    public ValidationResult validate(Command cmd) {
        return ValidationPassed;
    }
}
