package transport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.SequencerTestFixture;

public class SingleHostSequencerRoundTripTest {

    private SequencerTestFixture fixture;

    @BeforeEach
    public void before() {
        fixture = new SequencerTestFixture();
        fixture.before();
    }

    @Test
    public void singleMessageRoundTrip() {
        /**
         * Add logic to push a message through sequencer via
         * a command builder and assert it round trips through
         * sequencer.
         */
    }

    @AfterEach
    public void after() {
        fixture.after();
    }
}
