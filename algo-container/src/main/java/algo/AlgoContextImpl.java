package algo;

import atomic_broadcast.client.CommandProcessor;
import command.CommandBuilder;

public class AlgoContextImpl implements AlgoContext {

    private CommandProcessor cmdProcessor;
    private CommandBuilder cmdBuilder;

    public AlgoContextImpl(CommandProcessor cmdProcessor,
                           CommandBuilder cmdBuilder) {
        this.cmdProcessor = cmdProcessor;
        this.cmdBuilder = cmdBuilder;
    }

    public CommandProcessor cmdProcessor() {
        return cmdProcessor;
    }

    public CommandBuilder cmdBuilder() {
        return cmdBuilder;
    }
}
