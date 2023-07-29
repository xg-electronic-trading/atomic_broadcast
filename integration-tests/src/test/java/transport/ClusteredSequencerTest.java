package transport;

import atomic_broadcast.utils.EventReaderType;
import org.junit.jupiter.api.*;
import utils.SequencerTestFixture;

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


}
