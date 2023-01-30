package validator;

import atomic_broadcast.client.CommandValidator;
import atomic_broadcast.utils.ValidationResult;
import command.Command;

public class AlgoCommandValidator implements CommandValidator {
    @Override
    public ValidationResult validate(Command cmd) {
        return null;
    }
}
