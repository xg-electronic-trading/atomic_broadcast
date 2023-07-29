package transport;

import atomic_broadcast.utils.EventReaderType;
import org.junit.jupiter.api.*;
import utils.SequencerTestFixture;

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
    }

    @Test
    public void followerDropsThenRejoinsScenario() {
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

    }

    @Test
    public void leaderDropsThenFollowerAssumesLeadershipThenLeaderRejoinsScenario() {

    }


}
