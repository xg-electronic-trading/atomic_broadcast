package utils;

import atomic_broadcast.client.ClientPublisherModule;
import atomic_broadcast.client.CommandPublisher;
import atomic_broadcast.client.EventReaderModule;
import atomic_broadcast.host.Host;
import atomic_broadcast.sequencer.SequencerModule;
import atomic_broadcast.utils.*;
import atomic_broadcast.utils.Module;
import io.aeron.CommonContext;
import listener.EventPrinter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.fail;
import static utils.AsyncAssertions.pollUntil;

public class SequencerTestFixture {

    private Host hostA;

    public void before() {
        before(EventReaderType.Direct);
    }

    public void before(EventReaderType eventReaderType) {
        System.setProperty(CommonContext.DEBUG_TIMEOUT_PROP_NAME, "300s");

        hostA = new Host("hostA");

        TransportParams clientParams = TestTransportParams.createClientParams();
        clientParams.withEventReader(eventReaderType);

        hostA.deployShmSeqNoServer()
                .deployMediaDriver()
                .deploySequencer(TestTransportParams.createSequenceParams())
                .deployClient(clientParams, new EventPrinter())
                .start();

        pollSequencer(TransportState.PollCommandStream);
        pollPublisher(TransportState.ConnectedToCommandStream);
        pollClientTransport(TransportState.PollEventStream);
    }

    public CommandPublisher cmdPublisher() {
        return hostA.publisher().cmdPublisher();
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
            pollUntil(hostA.pollables(), expected, seq::state);
        }
    }

    public void pollClientTransport(TransportState expected) {
        Module module = findModule(ModuleName.ClientTransport);
        if (module instanceof EventReaderModule) {
            EventReaderModule eventBus = (EventReaderModule) module;
            pollUntil(hostA.pollables(), expected, eventBus::state);
        }
    }

    public void pollUntilCommandAcked(long id) {
        Module module = findModule(ModuleName.ClientTransport);
        if (module instanceof EventReaderModule) {
            EventReaderModule eventBus = (EventReaderModule) module;
            if (eventBus.listener() instanceof EventPrinter) {
                EventPrinter eventPrinter = (EventPrinter) eventBus.listener();
                pollUntil(hostA.pollables(), () -> eventPrinter.isCommandAcked(id));
            } else {
                fail("Cannot find EventPrinter MessageListener");
            }
        }
    }

    public void pollPublisher(TransportState expected) {
        Module module = findModule(ModuleName.ClientPublisher);
        if(module instanceof ClientPublisherModule) {
            ClientPublisherModule publisher = (ClientPublisherModule) module;
            pollUntil(hostA.pollables(), expected, publisher::state);
        }
    }


    @AfterEach
    public void after() {
        hostA.close();
    }
}
