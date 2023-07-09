package utils;

import atomic_broadcast.client.ClientPublisherModule;
import atomic_broadcast.client.CommandPublisher;
import atomic_broadcast.client.EventReaderModule;
import atomic_broadcast.consensus.ConsensusModule;
import atomic_broadcast.consensus.ConsensusWorker;
import atomic_broadcast.sequencer.SequencerTransportWorker;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import host.Host;
import atomic_broadcast.sequencer.SequencerModule;
import atomic_broadcast.utils.*;
import atomic_broadcast.utils.Module;
import io.aeron.CommonContext;
import listener.EventPrinter;
import org.agrona.IoUtil;
import org.junit.jupiter.api.AfterEach;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static atomic_broadcast.consensus.ClusterTransportState.Leader;
import static atomic_broadcast.utils.ShmFileConstants.SEQ_NUM_FILE_PREFIX;
import static atomic_broadcast.utils.ShmFileConstants.SHM_SUFFIX;
import static atomic_broadcast.utils.TransportState.*;
import static org.junit.jupiter.api.Assertions.fail;
import static utils.AsyncAssertions.pollUntil;

public class SequencerTestFixture {

    private final Log log = LogFactory.getLog(this.getClass().getName());

    private final List<Host> hosts = new ArrayList<>(10);

    public void before() {
        before(EventReaderType.Direct, 1);
    }

    public void before(EventReaderType eventReaderType, int numSequencers) {
        System.setProperty(CommonContext.DEBUG_TIMEOUT_PROP_NAME, "300s");

        for (int i = 0; i < numSequencers; i++) {
            int instance = i + 1;
            IoUtil.delete(new File(SEQ_NUM_FILE_PREFIX + instance + SHM_SUFFIX), false);
            Host host = new Host(instance);
            TransportParams clientParams = TestTransportParams.createClientParams();
            clientParams.withEventReader(eventReaderType).instance(instance);

            host.deployMediaDriver(instance)
                .deploySequencer(TestTransportParams.createSequencerParams().instance(instance), TestTransportParams.createConsensusParams().instance(instance))
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
            pollSequencer(PollCommandStream, h);
            pollPublisher(TransportState.ConnectedToCommandStream, h);
            pollClientTransport(TransportState.PollEventStream, h);
        });
    }

    public void pollUntilAny(Predicate<Module> predicate) {
        List<Pollable> allPollables = hosts.stream()
                .flatMap(hosts -> hosts.pollables().stream())
                .collect(Collectors.toList());

        List<Module> allModules = hosts.stream()
                .flatMap(host -> host.moduleList().stream())
                .collect(Collectors.toList());

        pollUntil(allPollables, () -> allModules.stream().anyMatch(predicate));
    }

    public void pollUntilAll(Predicate<Module> moduleFilter, Predicate<Module> predicate) {
        List<Pollable> allPollables = hosts.stream()
                .flatMap(hosts -> hosts.pollables().stream())
                .collect(Collectors.toList());

        List<Module> allModules = hosts.stream()
                .flatMap(host -> host.moduleList().stream())
                .collect(Collectors.toList());

        pollUntil(allPollables, () -> allModules.stream().filter(moduleFilter).allMatch(predicate));
    }

    public Predicate<Module> findLeaderPred = m -> {
        if (m instanceof ConsensusModule) {
            ConsensusModule consensus = (ConsensusModule) m;
            return consensus.consensusState().isLeader();
        }
        return false;
    };

    public Predicate<Module> commandBusConnected = m -> {
        if (m instanceof SequencerModule) {
            SequencerModule seq = (SequencerModule) m;
            return seq.state() == PollCommandStream;
        }
        return false;
    };

    public Predicate<Module> startReplay = m -> {
        if (m instanceof SequencerModule) {
            SequencerModule seq = (SequencerModule) m;
            return seq.state() == StartReplay;
        }
        return false;
    };

    public Predicate<Module> pollReplay = m -> {
        if (m instanceof SequencerModule) {
            SequencerModule seq = (SequencerModule) m;
            return seq.state() == PollReplay;
        }
        return false;
    };

    public Predicate<Module> findFollowerPred = m -> {
        if (m instanceof ConsensusModule) {
            ConsensusModule consensus = (ConsensusModule) m;
            return consensus.consensusState().isFollower();
        }
        return false;
    };

    public Predicate<Module> pollEventStream = m -> {
        if (m instanceof EventReaderModule) {
            EventReaderModule eventReaderModule = (EventReaderModule) m;
            return eventReaderModule.state() == PollEventStream;
        }
        return false;
    };

    public Predicate<Module> eventReaders = m -> m instanceof EventReaderModule;

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

    public void stopLeader() {
        hosts.stream()
                .filter(h -> h.consensus().consensusState().isLeader())
                .forEach(h -> {
                            List<Module> modulesToClose =  h.modules()
                                    .getModules()
                                    .stream()
                                    .filter(sequencerAppPred)
                                    .collect(Collectors.toList());

                            Collections.reverse(modulesToClose); //close modules in order of addition.
                            modulesToClose
                                    .forEach(m -> {
                                        log.info().append("closing module: ").appendLast(m.name());
                                        m.close();
                                            });

                            h.modules().getModules().forEach(m -> log.info().append("module (pre-removal): ").appendLast(m.name()));
                            h.modules().getModules().removeIf(sequencerAppPred);
                            h.modules().getModules().forEach(m -> log.info().append("module (post-removal): ").appendLast(m.name()));

                            h.pollables().forEach(p -> log.info().append("pollable (pre-removal): ").appendLast(p));
                            h.pollables().removeIf(p -> p instanceof SequencerTransportWorker || p instanceof ConsensusWorker);
                            h.pollables().forEach(p -> log.info().append("pollable (post-removal): ").appendLast(p));
                        }
                );
    }

    private final Predicate<Module> sequencerAppPred = m -> m.instanceInfo().app() == App.Sequencer;

    @AfterEach
    public void after() {
        hosts.forEach(Host::close);
    }
}
