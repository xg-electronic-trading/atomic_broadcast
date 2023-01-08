package utils;

import atomic_broadcast.client.EventBusTransportModule;
import atomic_broadcast.host.Host;
import atomic_broadcast.sequencer.SequencerModule;
import atomic_broadcast.utils.CompositeModule;
import atomic_broadcast.utils.Module;
import atomic_broadcast.utils.ModuleName;
import atomic_broadcast.utils.TransportState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Optional;

import static utils.AsyncAssertions.pollUntil;

public class SequencerTestFixture {

    private Host hostA;

    @BeforeEach
    public void before() {
        hostA = new Host("hostA");

        hostA.deployShmSeqNoServer()
                .deployMediaDriver()
                .deploySequencer(TestTransportParams.createSequenceParams())
                .deployClient(TestTransportParams.createClientParams())
                .start();

        pollSequencer(TransportState.PollCommandStream);
        pollClientTransport(TransportState.PollEventStream);
    }

    public Module findModule(ModuleName name) {
        Optional<Module> modOpt = hostA.moduleList()
                .stream()
                .filter(m -> m.name() == name)
                .findFirst();
        if (modOpt.isPresent()) {
            return modOpt.get();
        } else {
            throw new IllegalArgumentException("module " + name + " not found");
        }
    }

    public void pollSequencer(TransportState expected) {
        Module module = findModule(ModuleName.Sequencer);
        if (module instanceof SequencerModule) {
            SequencerModule seq = (SequencerModule) module;
            pollUntil(hostA.modules(), expected, seq::state);
        }
    }

    public void pollClientTransport(TransportState expected) {
        Module module = findModule(ModuleName.ClientTransport);
        if (module instanceof EventBusTransportModule) {
            EventBusTransportModule eventBus = (EventBusTransportModule) module;
            pollUntil(hostA.modules(), expected, eventBus::state);
        }
    }


    @AfterEach
    public void after() {
        hostA.close();
    }
}
