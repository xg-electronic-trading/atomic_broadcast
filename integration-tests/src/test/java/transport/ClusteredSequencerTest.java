package transport;

import atomic_broadcast.utils.EventReaderType;
import com.messages.sbe.OrdTypeEnum;
import com.messages.sbe.SideEnum;
import com.messages.sbe.StrategyEnum;
import com.messages.sbe.TimeInForceEnum;
import command.CommandBuilder;
import command.CommandBuilderImpl;
import command.NewOrderSingleCommandImpl;
import org.junit.jupiter.api.*;
import utils.SequencerTestFixture;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClusteredSequencerTest {

    private SequencerTestFixture fixture;

    public void before() {
        before(2);
    }
    public void before(int numSequencers) {
        fixture = new SequencerTestFixture();
        fixture.before(EventReaderType.Direct, numSequencers);
        fixture.start();

        fixture.pollUntilAny(fixture.findLeaderPred);
        fixture.pollUntilAny(fixture.findFollowerPred);
        fixture.pollUntilAny(fixture.commandBusConnected);
        fixture.pollUntilAny(fixture.startReplay);
        fixture.pollUntilAllFollowers(fixture.pollOpenEndedReplay);
        //poll until all clients have connected successfully
        fixture.pollUntilAll(fixture.eventReaders, fixture.pollEventStream);
    }

    @AfterEach
    public void after() {
        if (null != fixture) {
            fixture.after();
        }
    }

    @Test
    public void clusterMembersStartUpScenario() {
        before();
    }

    @Test
    public void leaderDropsThenRejoinsScenario() {
        before();
        sendMessages(5);
        fixture.stopLeader();
        fixture.startMostRecentStoppedSequencer();
        fixture.pollUntilAny(fixture.findLeaderPred);
        fixture.pollUntilAny(fixture.findFollowerPred);
        fixture.pollUntilAny(fixture.commandBusConnected);
        fixture.pollUntilAny(fixture.pollOpenEndedReplay);
        fixture.pollUntilAll(fixture.eventReaders, fixture.pollEventStream);
    }

    @Test
    public void followerDropsThenRejoinsScenario() {
        before();
        sendMessages(5);
        fixture.stopFollower();
        fixture.startMostRecentStoppedSequencer();
        fixture.pollUntilAny(fixture.findLeaderPred);
        fixture.pollUntilAny(fixture.findFollowerPred);
        fixture.pollUntilAny(fixture.commandBusConnected);
        fixture.pollUntilAny(fixture.pollOpenEndedReplay);
        fixture.pollUntilAll(fixture.eventReaders, fixture.pollEventStream);
    }

    @Test
    public void leaderDropsThenFollowerAssumesLeadershipScenario() {
        before(3);
        sendMessages(5);
        fixture.stopLeader();
        fixture.pollUntilAny(fixture.findLeaderPred);
        fixture.pollUntilAny(fixture.findFollowerPred);
        fixture.pollUntilAny(fixture.commandBusConnected);
        fixture.pollUntilAllFollowers(fixture.pollOpenEndedReplay);
        fixture.pollUntilAll(fixture.eventReaders, fixture.pollEventStream);
    }

    @Test
    public void leaderDropsThenFollowerAssumesLeadershipThenLeaderRejoinsScenario() {
        before(3);
        sendMessages(5);
        fixture.stopLeader();
        fixture.pollUntilAny(fixture.findLeaderPred);
        fixture.pollUntilAny(fixture.findFollowerPred);
        fixture.pollUntilAny(fixture.commandBusConnected);
        fixture.pollUntilAllFollowers(fixture.pollOpenEndedReplay);
        fixture.pollUntilAll(fixture.eventReaders, fixture.pollEventStream);

        fixture.startMostRecentStoppedSequencer();
        fixture.pollUntilAllFollowers(fixture.pollOpenEndedReplay);
        fixture.pollUntilAll(fixture.eventReaders, fixture.pollEventStream);

    }


    private void sendMessages(int numMessages) {
        CommandBuilder cmdBuilder = new CommandBuilderImpl();
        for (int i = 0; i < numMessages; i++) {
            NewOrderSingleCommandImpl nos = cmdBuilder.createNewOrderSingle();
            long id = i;
            nos.id(id)
                    .parentId(1234)
                    .symbol("VOD.L")
                    .side(SideEnum.Buy)
                    .transactTime(1_000)
                    .qty(1000)
                    .tif(TimeInForceEnum.Day)
                    .ordType(OrdTypeEnum.Limit)
                    .price(100_000L)
                    .strategy(StrategyEnum.LIQSEEK)
                    .exDest(5)
                    .startTime(1_000)
                    .endTime(1_001);

            assertTrue(fixture.sendCommand(nos));
            fixture.pollUntilCommandIdAcked(fixture.eventReaders, id);
        }
    }


}
