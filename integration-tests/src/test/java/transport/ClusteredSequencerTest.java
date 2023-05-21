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
    }

    @AfterEach
    public void after() {
        fixture.after();
    }

    @Test
    public void clusterMembersStartUpScenario() {
        fixture.pollUntilAny(fixture.findLeaderPred);
        fixture.pollUntilAny(fixture.findFollowerPred);
        fixture.pollUntilAny(fixture.commandBusConnected);
        fixture.pollUntilAny(fixture.startReplay);
        fixture.pollUntilAny(fixture.pollReplay);
        //poll until all clients have connected successfully
        fixture.pollUntilAll(fixture.eventReaders, fixture.pollEventStream);
    }

    @Test
    public void leaderDropsThenRejoinsScenario() {

    }

    @Test
    public void followerDropsThenRejoinsScenario() {

    }


}
