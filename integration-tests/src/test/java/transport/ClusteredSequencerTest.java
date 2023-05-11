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
        fixture.pollAllUntilLeaderElected();
    }

    @Test
    public void leaderDropsThenRejoinsScenario() {

    }

    @Test
    public void followerDropsThenRejoinsScenario() {

    }


}
