package atomic_broadcast.sample;

import atomic_broadcast.client.CommandProcessor;
import atomic_broadcast.listener.MessageListener;
import atomic_broadcast.transport.TransportFactory;
import atomic_broadcast.utils.*;

public class SampleClient extends AbstractClient {

    /**
     * Sample Client is an application that connects to the event stream
     * and prints events received.
     *
     * There is also a method to be able to send commands to the sequencer through
     * the command processor.
     */

    public SampleClient(InstanceInfo instanceInfo,
                        TransportParams transportParams,
                        TransportFactory transportFactory,
                        MessageListener messageListener,
                        ModuleBuilder moduleBuilder,
                        PollableBuilder pollableBuilder) {
        super(instanceInfo, transportParams, transportFactory, messageListener, moduleBuilder, pollableBuilder);
    }

    public CommandProcessor cmdProcessor() {
        return this.commandProcessor();
    }
}
