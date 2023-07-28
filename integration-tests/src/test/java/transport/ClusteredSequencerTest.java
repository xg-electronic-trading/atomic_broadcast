package transport;

import atomic_broadcast.client.CommandProcessor;
import atomic_broadcast.client.CommandProcessorImpl;
import atomic_broadcast.client.CommandPublisher;
import atomic_broadcast.client.NoOpCommandValidator;
import atomic_broadcast.utils.Action;
import atomic_broadcast.utils.EventReaderType;
import atomic_broadcast.utils.InstanceInfo;
import com.messages.sbe.OrdTypeEnum;
import com.messages.sbe.SideEnum;
import com.messages.sbe.StrategyEnum;
import com.messages.sbe.TimeInForceEnum;
import command.CommandBuilder;
import command.CommandBuilderImpl;
import command.NewOrderSingleCommandImpl;
import org.junit.jupiter.api.*;
import utils.SequencerTestFixture;

import static atomic_broadcast.utils.Action.CommandSent;
import static atomic_broadcast.utils.App.AlgoContainer;

public class ClusteredSequencerTest {

    private SequencerTestFixture fixture;

    @BeforeEach
    public void before() {
        fixture = new SequencerTestFixture();
        fixture.before(EventReaderType.Direct, 2);
        fixture.start();

        fixture.pollUntilAny(fixture.findLeaderPred);
        fixture.pollUntilAny(fixture.findFollowerPred);
        fixture.pollUntilAny(fixture.commandBusConnected);
        fixture.pollUntilAny(fixture.startReplay);
        fixture.pollUntilAny(fixture.pollOpenEndedReplay);
        //poll until all clients have connected successfully
        fixture.pollUntilAll(fixture.eventReaders, fixture.pollEventStream);
    }

    @AfterEach
    public void after() {
        fixture.after();
    }

    @Test
    public void clusterMembersStartUpScenario() {}

    @Test
    public void leaderDropsThenRejoinsScenario() {
        fixture.stopLeader();
        fixture.startMostRecentStoppedSequencer();
        fixture.pollUntilAny(fixture.findLeaderPred);
        fixture.pollUntilAny(fixture.findFollowerPred);
        fixture.pollUntilAny(fixture.commandBusConnected);
        fixture.pollUntilAny(fixture.pollOpenEndedReplay);
        fixture.pollUntilAll(fixture.eventReaders, fixture.pollEventStream);

        /**
         * TODO: Check follower can reconnect to src archive and continue
         * open ended replay after leader bounce
         */
    }

    @Test
    public void followerDropsThenRejoinsScenario() {

    }

    @Test
    public void leaderDropsThenFollowerAssumesLeadershipScenario() {

    }

    @Test
    public void leaderDropsThenFollowerAssumesLeadershipThenLeaderRejoinsScenario() {

    }


}
