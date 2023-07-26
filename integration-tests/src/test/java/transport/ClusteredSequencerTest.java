package transport;

import atomic_broadcast.utils.EventReaderType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
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
        fixture.pollUntilAny(fixture.pollReplay);
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
        fixture.pollUntilAll(fixture.eventReaders, fixture.pollEventStream);

        /**
         * TODO: Check follower can reconnect to src archive and continue
         * open ended replay after leader bounce
         *
         * TODO: check when a follower becomes leader it can find latest local
         * recording to extend.
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
