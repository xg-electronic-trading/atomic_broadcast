package utils;

import atomic_broadcast.client.ClientPublisherModule;
import atomic_broadcast.client.CommandPublisher;
import atomic_broadcast.client.EventReaderModule;
import atomic_broadcast.consensus.ConsensusModule;
import atomic_broadcast.host.Host;
import atomic_broadcast.sequencer.SequencerModule;
import atomic_broadcast.utils.*;
import atomic_broadcast.utils.Module;
import io.aeron.CommonContext;
import listener.EventPrinter;
import org.junit.jupiter.api.AfterEach;

import java.util.*;
import java.util.stream.Collectors;

import static atomic_broadcast.consensus.ClusterTransportState.Leader;
import static org.junit.jupiter.api.Assertions.fail;
import static utils.AsyncAssertions.pollUntil;

public class SequencerTestFixture {

    private final List<Host> hosts = new ArrayList<>(10);

    public void before() {
        before(EventReaderType.Direct, 1);
    }

    public void before(EventReaderType eventReaderType, int numSequencers) {
        System.setProperty(CommonContext.DEBUG_TIMEOUT_PROP_NAME, "300s");

        for (int i = 0; i < numSequencers; i++) {
            Host host = new Host(i + 1);
            TransportParams clientParams = TestTransportParams.createClientParams();
            clientParams.withEventReader(eventReaderType).instance(i+1);

            host.deployMediaDriver()
                .deploySequencer(TestTransportParams.createSequencerParams().instance(i+1), TestTransportParams.createConsensusParams().instance(i+1))
                .deployClient(clientParams, new EventPrinter());

            hosts.add(host);
        }
    }

    public void setLeader(int instance) {
        hosts.stream()
                .filter(h -> h.hostNum() == instance)
                .forEach(h -> {
                    h.consensus().consensusState().setState(Leader);
                    h.consensus().consensusState().setLeaderInstance(instance);
                });
    }

    public void start() {
        hosts.forEach(Host::start);
    }

    public void pollStandAloneSequencer() {
        hosts.forEach(h -> {
            pollSequencer(TransportState.PollCommandStream, h);
            pollPublisher(TransportState.ConnectedToCommandStream, h);
            pollClientTransport(TransportState.PollEventStream, h);
        });
    }

    public void pollAllUntilLeaderElected() {
        List<Pollable> allPollables = hosts.stream()
                .flatMap(hosts -> hosts.pollables().stream())
                .collect(Collectors.toList());

        List<Module> allModules = hosts.stream()
                .flatMap(host -> host.moduleList().stream())
                .collect(Collectors.toList());

        pollUntil(allPollables, () -> {
            Module module = findModule(ModuleName.Consensus, allModules);
            if (module instanceof ConsensusModule) {
                ConsensusModule consensus = (ConsensusModule) module;
                return consensus.consensusState().isLeader();
            }
            return false;
        });
    }

    public CommandPublisher cmdPublisher() {
        Optional<Host> hostOpt = hosts.stream().filter(h -> h.hostNum() == 1).findFirst();
        if (hostOpt.isPresent()) {
            return hostOpt.get().publisher().cmdPublisher();
        } else {
            throw new IllegalArgumentException("host-0 not found");
        }
    }

    public CommandPublisher cmdPublisher(Host host) {
        return host.publisher().cmdPublisher();
    }

    private Module findModule(ModuleName name, List<Module> modules) {
        Optional<Module> modOpt = modules
                .stream()
                .filter(m -> m.name() == name)
                .findFirst();
        if (modOpt.isPresent()) {
            return modOpt.get();
        } else {
            throw new IllegalArgumentException("module " + name + " not found");
        }
    }

    public Module findModule(ModuleName name, Host host) {
        return findModule(name, host.moduleList());
    }

    private void pollSequencer(TransportState expected, Host host) {
        Module module = findModule(ModuleName.Sequencer, host);
        if (module instanceof SequencerModule) {
            SequencerModule seq = (SequencerModule) module;
            pollUntil(host.pollables(), expected, seq::state);
        }
    }

    private void pollClientTransport(TransportState expected, Host host) {
        Module module = findModule(ModuleName.ClientTransport, host);
        if (module instanceof EventReaderModule) {
            EventReaderModule eventBus = (EventReaderModule) module;
            pollUntil(host.pollables(), expected, eventBus::state);
        }
    }

    public void pollUntilCommandAcked(long id) {
        hosts.forEach(h -> {
            pollUntilCommandAcked(id, h);
        });
    }

    public void pollUntilCommandAcked(long id, Host host) {
        Module module = findModule(ModuleName.ClientTransport, host);
        if (module instanceof EventReaderModule) {
            EventReaderModule eventBus = (EventReaderModule) module;
            if (eventBus.listener() instanceof EventPrinter) {
                EventPrinter eventPrinter = (EventPrinter) eventBus.listener();
                pollUntil(host.pollables(), () -> eventPrinter.isCommandAcked(id));
            } else {
                fail("Cannot find EventPrinter MessageListener");
            }
        }
    }

    private void pollPublisher(TransportState expected, Host host) {
        Module module = findModule(ModuleName.ClientPublisher, host);
        if(module instanceof ClientPublisherModule) {
            ClientPublisherModule publisher = (ClientPublisherModule) module;
            pollUntil(host.pollables(), expected, publisher::state);
        }
    }


    @AfterEach
    public void after() {
        hosts.forEach(Host::close);
    }
}
